# Documentation HAPI FHIR JPA Server
## Introduction
The purpose of this HAPI FHIR JPA Server is to provide a Proof of Concept where it functions like a metadata register. HAPI FHIR is a complete implementation of the HL7 FHIR standard for healthcare interoperability in Java. It is an open source software licensed under the business-friendly Apache Software License 2.0.
Links:  
Home: https://hapifhir.io/
Docs: https://hapifhir.io/hapi-fhir/docs/
## JPA server
Link: https://github.com/hapifhir/hapi-fhir-jpaserver-starter

The HAPI FHIR JPA server provides a persistence module which can be used to provide a complete RESTful server implementation, backed by a database of your choosing. This module uses the JPA 2.0 API to store data in a database without depending on any specific database technology. This project is a fully contained FHIR server, supporting all standard operations (read/create/delete/etc). It bundles an embedded instance of the H2 Java Database so that the server can run without depending on any external database, but it can also be configured to use an installation of Oracle, Postgres, etc.
This project is a complete starter project you can use to deploy an FHIR server using HAPI FHIR JPA. It is specifically intended for end users of the HAPI FHIR JPA server module (in other words, it helps you implement HAPI FHIR, it is not the source of the library itself). If you are looking for the main HAPI FHIR project, see here: https://github.com/hapifhir/hapi-fhir
While this project shows how you can use many parts of the HAPI FHIR framework, there are a set of features which you should be aware of are missing. Or features you need to supply yourself or get professional support ahead of using it directly in production. For example, the service as is does not provide any security or enterprise audit logging.

**HAPI JPA Server has the following components:** ![](https://lh7-rt.googleusercontent.com/docsz/AD_4nXdL7rKrfOpq7myofkaXeSW-Pv1i7pHQe53rX1v61pa_ls7IC6UL2JIKZ5Xtd8OEB31l3NRfjLBQvSW0qinkDcz-Jw500DjTRkHicwuIv3GzNl6Y3Ldz3mtII8FA-Arq2v1VLKxPWk_HKutRQL6C6j7AU41G?key=flRIJ62mIRWJ10ofPwigtw)
**Resource Providers:** A RESTful server Resource Provider is provided for each resource type in a given release of FHIR. Each resource provider implements a @Search method implementing the complete set of search parameters defined in the FHIR specification for the given resource type.
The resource providers also extend a superclass which implements all the other FHIR methods, such as Read, Create, Delete, etc.
Note that these resource providers are generated as a part of the HAPI build process, so they are not checked into Git. The resource providers do not implement any of the logic in searching, updating, etc. They simply receive the incoming HTTP calls (via the RestfulServer) and pass along the incoming requests to the DAOs.
**HAPI DAOs:** The DAOs actually implement all the database business logic relating to the storage, indexing, and retrieval of FHIR resources, using the underlying JPA API.
**Hibernate:** The HAPI JPA Server uses the JPA library, implemented by Hibernate. No Hibernate specific features are used, so the library should also work with other providers (e.g. Eclipselink) but it is not tested regularly with them.
**Database:** The RESTful server uses an embedded database but can be configured to talk to any database supported by Hibernate. The JPA Server maintains active support for several databases such as: MS SQL Server, PostgreSQL, ORACLE.

**Other parts that can be implemented:**
These parts are not completely implemented but a built-in architecture is supplied for the users to set it up.

**Interceptors:** Interceptor classes may "hook into" various points in the processing chain in both the client and the server. The interceptor framework has been designed to be flexible enough to hook into almost every part of the library. When trying to figure out "how would I make HAPI FHIR do X", the answer is very often to create an interceptor.
**Server Security:** Every system and architecture operates in a different set of rules, and has different security requirements. As such, HAPI FHIR does not provide a single one-size-fits-all security layer. Instead, it provides a number of useful tools and building blocks that can be built around as a part of your overall security architecture.

## Basic FHIR model Hints
**Extensions** provide a standardized way of placing additional data in a resource. Extensions do not necessarily point to other resources but only act as an extra information holder.
**Resource references** reference to a different resource. I.e. an ImagingStudy can have a resource reference to a Practioner, an Organization, Patient. This way the resources are linked with each other.
## JPA Server setup as Metadata register
To get started:
1. Clone or fork the JPA server starter GitHub repository: https://github.com/thomas-samoht/hapi-fhir-jpaserver-starter
2. Install a JDK: Minimum JDK17 or newer.
   A good Open JDK is Azul Zulu: https://www.azul.com/downloads/?package=jdk#zulu
3. Install Apache Maven build tool
   Used if you don't use an editor with Maven support. https://maven.apache.org/download.cgi
4. Depending on how you want to run the JPA server:
    1. Install Docker if you want to run the database in PostgreSQL: https://docs.docker.com/engine/install/
    2. If you dont mind to use the default in-memory H2 database, then replace in `application.yaml` the datasource with:

```
  datasource:
    #url: 'jdbc:h2:file:./target/database/h2'
    url: jdbc:h2:mem:test_mem
    username: sa
    password: null
    driverClassName: org.h2.Driver
    max-active: 15

    # database connection pool size
    hikari:
      maximum-pool-size: 10
```

And replace the value of `hibernate.dialect` with `ca.uhn.fhir.jpa.model.dialect.HapiFhirH2Dialect`
If you choose the Docker route run `docker compose up` in the terminal to create a database container.
With these steps complete this should be enough to be able to start the basic JPA server by running: ```mvn spring-boot:run```
To test it out try to do POST and GET requests to the endpoint that is by default set to `http://localhost:8080/fhir`

To test it with the Timeline service, add at least an ImagingStudy to the HAPI fhir database.
To easily set up sample fhir entries use the following steps:
1. Copy `bundle.json` from `fhir_examples`
2. Do POST request to base URL i.e. `http://localhost:8080/fhir` - Paste the bundle inside the body of the request

Before you do the request to the timeline service ensure your endpoint is registered and the pseudonym exchange service knows your provider-id

### application.yaml file
In the `src/main/resources/application.yaml` file, you can configure server and HAPI settings and enable or disable components, including custom resource providers and interceptors.
The endpoint of the Pseudonym service is defined in the `application.yaml` file `pseudonym-exchange-service`. Here the target provider ID can be set as well.

**Custom Resource Providers:**
- **`PatientResourceProvider`**: Overrides the default create method to add a pseudonym as an UUID in the Patient resource.
- **`ImagingStudyResourceProvider`**: Modifies the search method to handle pseudonyms, which are processed through a pseudonym exchange service to match patients and retrieve related imaging studies.
**Custom Interceptor**
- `RemovePatientExtensionsInterceptor`: Removes the pseudonym extension from Patients before giving a response. This is important since the pseudonym should only be used internally.

![](https://public.images.stashpad.live/wuSV7B9TYtAjBQB0Woh2v03a)

## TODO's
- [ ] Instead of an Interceptor an separate database can be used to store the patient id with pseudonym to ensure the pseudonym is never shown in any response, but it can still be used to query for patients.
- [ ] Add parts...