# perspicuus, a metadata catalog system.

#### Overview

The system stores *Schemas* (currently Avro, eventually polyglot).

Schemas are organized into *Subjects*, which are named sequences of Schema versions, such as a single table or file format evolving over time.

In addition to being part of a Subject, each Schema can be placed in any number of *SchemaGroups*
These may be used for various organizational purposes, such as to collect all the Schemas for a given database, project or user.

Both Schemas and SchemaGroups may be *annotated* by any number of key-value pairs.
These provide a general mechanism for attaching additional information which is outside of the Schema's native format, such as design or usage insights.

#### Usage: Server

The server component exposes a REST/JSON API.  The technology stack is wildfly-swarm (resteasy, hibernate, hibernate-search).

    cd server
    mvn clean compile verify
    java -jar target/server-0.1.0-SNAPSHOT-swarm.jar

By default an in-memory H2 database is used. For production use rewire the database via edits to pom.xml, project-stages.yml and persistence.xml
in accordance with https://howto.wildfly-swarm.io/create-a-datasource/

The server also uses hibernate-search for full text indexing. Edit persistence.xml to use filesystem storage instead of RAM for this.

### Usage: Client

Clients may use the REST API directly, but there is also a Java wrapper for it provided in the client module.

        <dependency>
            <groupId>org.jboss.perspicuus</groupId>
            <artifactId>client</artifactId>
            <version>0.1.0-SNAPSHOT</version>
        </dependency>

usage example:
```
import org.jboss.perspicuus.client.*;
```
```
SchemaRegistryClient schemaRegistryClient = new SchemaRegistryClient("http://localhost:8080");
Schema schema = ...
long schemaId = schemaRegistryClient.registerSchema(schema.getName(), schema.toString());
schemaRegistryClient.annotate(schemaId,"key", "value");
long groupId = schemaRegistryClient.createGroup();
schemaRegistryClient.addSchemaToGroup(groupId, schemaId);
schemaRegistryClient.annotate(groupId, "key", "value");
```

The client additionally provides adaptors for working with schema from two sources: Spark and JDBC.

### Related reading and projects:

http://atlas.apache.org/

https://github.com/confluentinc/schema-registry

http://docs.spring.io/spring-cloud-stream/docs/Brooklyn.M1/reference/htmlsingle/#_schema_registry_server

https://aws.amazon.com/glue/

https://github.com/airbnb/knowledge-repo

https://github.com/Netflix/metacat

https://github.com/linkedin/WhereHows

http://cidrdb.org/cidr2017/papers/p44-deng-cidr17.pdf The Data Civilizer System

http://dl.acm.org/citation.cfm?id=2903730  Goods: Organizing Google's Datasets

http://cidrdb.org/cidr2017/papers/p111-hellerstein-cidr17.pdf Ground: A Data Context Service

https://finraos.github.io/herd/

https://github.com/yelp/schematizer