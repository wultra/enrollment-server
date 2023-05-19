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

## API Extensions

Since version `1.5.0`, the API supports new cell types in operation responses. These are:

- `IMAGE` - Cell with image.
- `ALERT` - Cell with alert banner.
- `AMOUNT_CONVERSION` - Cell with amount conversion between currencies.

See the documentation for attribute details:

- [Mobile Token API](./Mobile-Token-API.md)
- [Operation Form Data](./Operation-Form-Data.md)

## Dependencies

PostgreSQL JDBC driver is already included in the WAR file.
Oracle JDBC driver remains optional and must be added to your deployment if desired.
