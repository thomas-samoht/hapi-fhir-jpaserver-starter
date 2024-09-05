package ca.uhn.fhir.jpa.starter.ResourceProvider;

import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.jpa.starter.AppProperties;
import ca.uhn.fhir.rest.annotation.RequiredParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.hl7.fhir.r5.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Resource provider contains one or more methods which have been annotated with special annotations indicating
 * which RESTful operation that method supports.
 * SearchStudy requires a pseudonym from the LRS, this pseudonym is then exchanged for a provider unique pseudonym
 * after which ImagingStudies are retrieved that belong to patient with that pseudonym
 */
@Component
public class ImagingStudyResourceProvider implements IResourceProvider {

	private static final Logger log = LoggerFactory.getLogger(ImagingStudyResourceProvider.class);
	private final IFhirResourceDao<ImagingStudy> imagingStudyDao;
	private final IFhirResourceDao<Patient> patientDao;
	private final AppProperties appProperties;

	public ImagingStudyResourceProvider(IFhirResourceDao<ImagingStudy> imagingStudyDao, IFhirResourceDao<Patient> patientDao, AppProperties appProperties) {
		this.imagingStudyDao = imagingStudyDao;
		this.patientDao = patientDao;
		this.appProperties = appProperties;

		if (appProperties.getPseudonymExchangeService() == null) {
			throw new IllegalStateException("Pseudonym service not set");
		}
	}

	/**
	 * Search method to search for ImagingStudies belonging to a patient
	 * usage: GET /fhir/ImagingStudy/_search?pseudonym=677b33c7-30e0-4fe1-a740-87fd73c4dfaf
	 *
	 * @param pseudonym
	 *    Query parameter called pseudonym of type UuidType is the only search criteria.
	 * @return
	 *    Returns a list of ImagingStudies belonging to the patient
	 */
	@Search
	public List<ImagingStudy> searchStudy(@RequiredParam(name = "pseudonym") UuidType pseudonym, RequestDetails theRequestDetails) {

		try {
			// First exchange pseudonym for provider unique pseudonym
			UuidType newPseudonym = exchangePseudonym(pseudonym);
			PatientResourceProvider patientResourceProvider = new PatientResourceProvider(patientDao);
			// Get patients by pseudonym
			List<Patient> patientList = patientResourceProvider.searchPatientByPseudonym(newPseudonym, theRequestDetails);

			// Retrieve and return imagingStudies that have patient set as reference
			return patientList.stream()
				.map(patient -> {
					SearchParameterMap paramMap = new SearchParameterMap();
					paramMap.add(ImagingStudy.SP_PATIENT, new ReferenceParam(patient.getIdElement().getValue()));
					IBundleProvider results = imagingStudyDao.search(paramMap, theRequestDetails);
					return results != null ? results.getResources(0, results.size()) : Collections.emptyList();
				})
				.flatMap(Collection::stream)
				.map(resource -> (ImagingStudy) resource)
				.collect(Collectors.toList());
		} catch (Exception e) {
			log.error(e.getMessage());
			return new ArrayList<>();
		}
	}

	/**
	 * Search method to search for ImagingStudies belonging to a patient
	 * usage: GET /fhir/ImagingStudy/_search?pseudonym=677b33c7-30e0-4fe1-a740-87fd73c4dfaf
	 *
	 * @param oldPseudonym
	 *    Pseudonym that is given to exchange service
	 * @return
	 *    Pseudonym that is returned by exchange service
	 */
	private UuidType exchangePseudonym(UuidType oldPseudonym) throws IOException {
		String sourcePseudonym = oldPseudonym.getValue();
		String endpoint = appProperties.getPseudonymExchangeService().getEndpoint();
		String targetProviderId = appProperties.getPseudonymExchangeService().getTargetProviderId();

		try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {
			HttpPost request = new HttpPost(endpoint);

			// Manually create the JSON string
			String json = "{"
				+ "\"target_provider_id\": \"" + targetProviderId + "\","
				+ "\"source_pseudonym\": \"" + sourcePseudonym + "\""
				+ "}";

			StringEntity params = new StringEntity(json);
			request.addHeader("accept", "application/json");
			request.addHeader("Content-Type", "application/json");
			request.setEntity(params);

			CloseableHttpResponse response = httpClient.execute(request);
			String responseString = EntityUtils.toString(response.getEntity());
			// Regex to retrieve only the UUID
			String pseudonym = responseString.replaceAll(".*\"pseudonym\"\\s*:\\s*\"(.*?)\".*", "$1");
			httpClient.close(); // Ensure the client is closed
			return new UuidType(pseudonym);
		} catch (Exception ex) {
			log.info("{} Stacktrace: {}", ex.getMessage(), Arrays.toString(ex.getStackTrace()));
			return null;
		}
	}

	@Override
	public Class<ImagingStudy> getResourceType() {
		return ImagingStudy.class;
	}
}
