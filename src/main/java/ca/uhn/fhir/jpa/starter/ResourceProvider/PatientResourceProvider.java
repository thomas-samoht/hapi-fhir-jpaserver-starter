package ca.uhn.fhir.jpa.starter.ResourceProvider;

import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.rest.annotation.*;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.IResourceProvider;
import org.hl7.fhir.r4.model.*;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class PatientResourceProvider implements IResourceProvider {

	private final IFhirResourceDao<Patient> patientDao;

	public PatientResourceProvider(IFhirResourceDao<Patient> patientDao) {
		this.patientDao = patientDao;
	}

	@Create
	public MethodOutcome createPatient(@ResourceParam Patient thePatient, RequestDetails theRequestDetails) {
		String uuid = UUID.randomUUID().toString();
		Extension ext = new Extension();
		ext.setUrl("https://example.com/extensions#pseudonym");
		ext.setValue(new UuidType(uuid));
		thePatient.addExtension(ext);
		return patientDao.create(thePatient, theRequestDetails);
	}

	@Search
	public List<Patient> searchPatientByPseudonym(@RequiredParam(name = "pseudonym") UuidType pseudonym, RequestDetails theRequestDetails) {
		SearchParameterMap params = new SearchParameterMap();
		return patientDao.searchForResources(params, theRequestDetails).stream()
				.filter(patient -> patient.getExtension().stream()
						.anyMatch(ext -> Objects.equals(ext.getValue().primitiveValue(), pseudonym.getValue())))
				.collect(Collectors.toList());
	}


	@Override
	public Class<Patient> getResourceType() {
		return Patient.class;
	}
}
