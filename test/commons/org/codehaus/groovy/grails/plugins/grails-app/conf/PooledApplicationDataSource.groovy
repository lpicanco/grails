class PooledApplicationDataSource {
    boolean pooling = true
    String dbCreate = "create-drop" // one of 'create', 'create-drop','update'
    String url = "jdbc:hsqldb:mem:testDB"
    String driverClassName = "org.hsqldb.jdbcDriver"
    String username = "sa"
    String password = ""
}
