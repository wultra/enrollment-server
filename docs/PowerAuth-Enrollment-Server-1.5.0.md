# Migration from 1.4.x to 1.5.x

This guide contains instructions for migration from PowerAuth Enrollment Server version `1.4.x` to version `1.5.0`.


## Spring Boot 3

The PowerAuth Enrollment Sever was upgraded to Spring Boot 3, Spring Framework 6, and Hibernate 6.
It requires Java 17 or newer.

Remove this property.

`spring.jpa.properties.hibernate.temp.use_jdbc_metadata_defaults=false`

Make sure that you use dialect without version.

```properties
# spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
# spring.jpa.database-platform=org.hibernate.dialect.OracleDialect
```


## Database Changes


### Drop MySQL Support

Since version `1.5.0`, MySQL database is not supported anymore.
