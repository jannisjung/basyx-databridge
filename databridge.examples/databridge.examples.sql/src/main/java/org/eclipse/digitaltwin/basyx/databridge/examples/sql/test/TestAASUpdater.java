package org.eclipse.digitaltwin.basyx.databridge.examples.sql.test;
import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.SimpleRegistry;
import org.apache.camel.component.sql.SqlComponent;

import java.util.List;
import java.util.Map;

import javax.sql.DataSource;
import org.sqlite.SQLiteDataSource;

public class TestAASUpdater {
    public static void main(String[] args) throws Exception {
        // Configure the SQLite datasource
        SQLiteDataSource dataSource = new SQLiteDataSource();
        dataSource.setUrl("jdbc:sqlite:data.db");

        // Register the datasource in Camel's registry
        SimpleRegistry registry = new SimpleRegistry();
        registry.bind("myDataSource", dataSource);

        // Create CamelContext with the registry
        CamelContext camelContext = new DefaultCamelContext(registry);

        // Add the SQL component and set the datasource
        SqlComponent sqlComponent = new SqlComponent();
        sqlComponent.setDataSource(dataSource);
        camelContext.addComponent("sql", sqlComponent);

        // Create and add Camel routes
        camelContext.addRoutes(new RouteBuilder() {
            @Override
            public void configure() {
                // Define a route that triggers every 10 seconds
                from("timer://foo?period=10000")
                    .to("sql:SELECT value, timestamp FROM random_values ORDER BY id DESC LIMIT 1")
                    .process(exchange -> {
                        // Retrieve the latest value from the database
                        List<Map<String, Object>> rows = exchange.getIn().getBody(List.class);
                        if (!rows.isEmpty()) {
                            Map<String, Object> latestRow = rows.get(0);
                            Object value = latestRow.get("value");
                            Object timestamp = latestRow.get("timestamp");
                            System.out.println("Latest value: " + value + ", Timestamp: " + timestamp);
                        } else {
                            System.out.println("No data found.");
                        }
                    });
            }
        });

        // Start the Camel context
        camelContext.start();

        // Keep the application running
        Thread.sleep(60000);

        // Stop the Camel context
        camelContext.stop();
    }

}
