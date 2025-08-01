package dev.spangler;

import dev.spangler.deserializer.JsonKeyValueDeserializationSchema;
import dev.spangler.dto.SalesPerCategory;
import dev.spangler.dto.SalesPerDay;
import dev.spangler.dto.SalesPerMonth;
import dev.spangler.dto.Transaction;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.connector.elasticsearch.sink.Elasticsearch7SinkBuilder;
import org.apache.flink.connector.jdbc.JdbcConnectionOptions;
import org.apache.flink.connector.jdbc.JdbcExecutionOptions;
import org.apache.flink.connector.jdbc.JdbcSink;
import org.apache.flink.connector.jdbc.JdbcStatementBuilder;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.elasticsearch7.shaded.org.apache.http.HttpHost;
import org.apache.flink.elasticsearch7.shaded.org.elasticsearch.action.index.IndexRequest;
import org.apache.flink.elasticsearch7.shaded.org.elasticsearch.client.Requests;
import org.apache.flink.elasticsearch7.shaded.org.elasticsearch.common.xcontent.XContentType;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

import java.sql.Date;

import static dev.spangler.utils.JsonUtil.convertTransactionToJson;

public class DataStreamJob {

    private static final String jdbcUrl = "jdbc:postgresql://localhost:5433/postgres";
    private static final String driverName = "org.postgresql.Driver";
    private static final String username = "postgres";
    private static final String password = "postgres";

    public static void main(String[] args) throws Exception {

        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        String topic = "financial_transactions";
        String bootstrapServer = "localhost:9092";
        String groupId = "flink-collection";

        KafkaSource<Transaction> transactionKafkaSource = KafkaSource.<Transaction>builder()
                .setBootstrapServers(bootstrapServer)
                .setTopics(topic)
                .setGroupId(groupId)
                .setStartingOffsets(OffsetsInitializer.earliest())
                .setValueOnlyDeserializer(new JsonKeyValueDeserializationSchema())
                .build();

        DataStream<Transaction> transactionDataStream = env.fromSource(
                transactionKafkaSource,
                WatermarkStrategy.noWatermarks(),
                "Kafka source"
        );

        transactionDataStream.print();

        JdbcExecutionOptions executionOptions = new JdbcExecutionOptions.Builder()
                .withBatchSize(1000)
                .withBatchIntervalMs(200)
                .withMaxRetries(5)
                .build();

        JdbcConnectionOptions connectionOptions = new JdbcConnectionOptions.JdbcConnectionOptionsBuilder()
                .withUrl(jdbcUrl)
                .withDriverName(driverName)
                .withUsername(username)
                .withPassword(password)
                .build();

        // Create transactions table
        transactionDataStream.addSink(JdbcSink.sink(
                "CREATE TABLE IF NOT EXISTS transactions (" +
                        "transaction_id VARCHAR(255) PRIMARY KEY, " +
                        "product_id VARCHAR(255), " +
                        "product_name VARCHAR(255), " +
                        "product_category VARCHAR(255), " +
                        "product_price DOUBLE PRECISION, " +
                        "product_quantity INTEGER, " +
                        "product_brand VARCHAR(255), " +
                        "currency VARCHAR(255), " +
                        "customer_id VARCHAR(255), " +
                        "transaction_date TIMESTAMP, " +
                        "payment_method VARCHAR(255), " +
                        "payment_status VARCHAR(255), " +
                        "total_amount DOUBLE PRECISION " +
                        ")",
                (JdbcStatementBuilder<Transaction>) (preparedStatement, transaction) -> {

                },
                executionOptions,
                connectionOptions
        )).name("Create transactions table");

        // Create sales_per_transaction table
        transactionDataStream.addSink(JdbcSink.sink(
                "CREATE TABLE IF NOT EXISTS sales_per_category (" +
                        "transaction_date DATE, " +
                        "category VARCHAR(255), " +
                        "total_sales DOUBLE PRECISION, " +
                        "PRIMARY KEY (transaction_date, category)" +
                        ")",
                (JdbcStatementBuilder<Transaction>) (preparedStatement, transaction) -> {

                },
                executionOptions,
                connectionOptions
        )).name("Create sales_per_transactions table");

        // Create sales_per_day table
        transactionDataStream.addSink(JdbcSink.sink(
                "CREATE TABLE IF NOT EXISTS sales_per_day (" +
                        "transaction_date DATE PRIMARY KEY, " +
                        "total_sales DOUBLE PRECISION" +
                        ")",
                (JdbcStatementBuilder<Transaction>) (preparedStatement, transaction) -> {

                },
                executionOptions,
                connectionOptions
        )).name("Create sales_per_day table");

        // Create sales_per_month table
        transactionDataStream.addSink(JdbcSink.sink(
                "CREATE TABLE IF NOT EXISTS sales_per_month (" +
                        "month INTEGER, " +
                        "year INTEGER, " +
                        "total_sales DOUBLE PRECISION, " +
                        "PRIMARY KEY (year, month)" +
                        ")",
                (JdbcStatementBuilder<Transaction>) (preparedStatement, transaction) -> {

                },
                executionOptions,
                connectionOptions
        )).name("Create sales_per_month table");

        // Insert value into transactions table, without duplicates
        transactionDataStream.addSink(JdbcSink.sink(
                "INSERT INTO transactions(transaction_id, product_id, product_name, product_category, " +
                        "product_price, product_quantity, product_brand, currency, customer_id, transaction_date, " +
                        "payment_method, payment_status, total_amount) " +
                        "VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                        "ON CONFLICT (transaction_id) DO UPDATE SET " +
                        "product_id = EXCLUDED.product_id, " +
                        "product_name = EXCLUDED.product_name, " +
                        "product_category = EXCLUDED.product_category, " +
                        "product_price = EXCLUDED.product_price, " +
                        "product_quantity = EXCLUDED.product_quantity, " +
                        "product_brand = EXCLUDED.product_brand, " +
                        "currency = EXCLUDED.currency, " +
                        "customer_id = EXCLUDED.customer_id, " +
                        "transaction_date = EXCLUDED.transaction_date, " +
                        "payment_method = EXCLUDED.payment_method, " +
                        "payment_status = EXCLUDED.payment_status, " +
                        "total_amount = EXCLUDED.total_amount " +
                        "WHERE transactions.transaction_id = EXCLUDED.transaction_id",
                (JdbcStatementBuilder<Transaction>) (preparedStatement, transaction) -> {
                    preparedStatement.setString(1, transaction.getTransactionId());
                    preparedStatement.setString(2, transaction.getProductId());
                    preparedStatement.setString(3, transaction.getProductName());
                    preparedStatement.setString(4, transaction.getProductCategory());
                    preparedStatement.setDouble(5, transaction.getProductPrice());
                    preparedStatement.setInt(6, transaction.getProductQuantity());
                    preparedStatement.setString(7, transaction.getProductBrand());
                    preparedStatement.setString(8, transaction.getCurrency());
                    preparedStatement.setString(9, transaction.getCustomerId());
                    preparedStatement.setTimestamp(10, transaction.getTransactionDate());
                    preparedStatement.setString(11, transaction.getPaymentMethod());
                    preparedStatement.setString(12, transaction.getPaymentStatus());
                    preparedStatement.setDouble(13, transaction.getTotalAmount());
                },
                executionOptions,
                connectionOptions
        )).name("Insert data into transactions table");

        // Insert value into sales_per_category table, without duplicates
        transactionDataStream.map(
                        transaction -> {
                            Date transactionDate = new Date(System.currentTimeMillis());
                            String category = transaction.getProductCategory();
                            double totalSales = transaction.getTotalAmount();

                            return new SalesPerCategory(transactionDate, category, totalSales);
                        }
                ).keyBy(SalesPerCategory::getCategory)
                .reduce((salesPerCategory, t1) -> {
                    salesPerCategory.setTotalSales(salesPerCategory.getTotalSales() + t1.getTotalSales());

                    return salesPerCategory;
                }).addSink(JdbcSink.sink(
                        "INSERT INTO sales_per_category (transaction_date, category, total_sales) " +
                                "VALUES (?, ?, ?) " +
                                "ON CONFLICT (transaction_date, category) DO UPDATE SET " +
                                "total_sales = EXCLUDED.total_sales " +
                                "WHERE sales_per_category.transaction_date = EXCLUDED.transaction_date " +
                                "AND sales_per_category.category = EXCLUDED.category",
                        (JdbcStatementBuilder<SalesPerCategory>) (preparedStatement, salesPerCategory) -> {
                            preparedStatement.setDate(1, new Date(System.currentTimeMillis()));
                            preparedStatement.setString(2, salesPerCategory.getCategory());
                            preparedStatement.setDouble(3, salesPerCategory.getTotalSales());
                        },
                        executionOptions,
                        connectionOptions
                )).name("Insert data into sales_per_category table");

        // Insert value into sales_per_day table, without duplicates
        transactionDataStream.map(
                        transaction -> {
                            Date transactionDate = new Date(System.currentTimeMillis());
                            double totalSales = transaction.getTotalAmount();

                            return new SalesPerDay(transactionDate, totalSales);
                        }
                ).keyBy(SalesPerDay::getTransactionDate)
                .reduce((salesPerDay, t1) -> {
                    salesPerDay.setTotalSales(salesPerDay.getTotalSales() + t1.getTotalSales());

                    return salesPerDay;
                }).addSink(JdbcSink.sink(
                        "INSERT INTO sales_per_day (transaction_date, total_sales) " +
                                "VALUES (?, ?) " +
                                "ON CONFLICT (transaction_date) DO UPDATE SET " +
                                "total_sales = EXCLUDED.total_sales " +
                                "WHERE sales_per_day.transaction_date = EXCLUDED.transaction_date ",
                        (JdbcStatementBuilder<SalesPerDay>) (preparedStatement, salesPerDay) -> {
                            preparedStatement.setDate(1, new Date(System.currentTimeMillis()));
                            preparedStatement.setDouble(2, salesPerDay.getTotalSales());
                        },
                        executionOptions,
                        connectionOptions
                )).name("Insert data into sales_per_day table");

        // Insert value into sales_per_month table, without duplicates
        transactionDataStream.map(
                        transaction -> {
                            Date transactionDate = new Date(System.currentTimeMillis());
                            int month = transactionDate.toLocalDate().getMonth().getValue();
                            int year = transactionDate.toLocalDate().getYear();
                            double totalSales = transaction.getTotalAmount();

                            return new SalesPerMonth(month, year, totalSales);
                        }
                ).keyBy(SalesPerMonth::getMonth)
                .reduce((salesPerMonth, t1) -> {
                    salesPerMonth.setTotalSales(salesPerMonth.getTotalSales() + t1.getTotalSales());

                    return salesPerMonth;
                }).addSink(JdbcSink.sink(
                        "INSERT INTO sales_per_month (month, year, total_sales) " +
                                "VALUES (?, ?, ?) " +
                                "ON CONFLICT (year, month) DO UPDATE SET " +
                                "total_sales = EXCLUDED.total_sales " +
                                "WHERE sales_per_month.month = EXCLUDED.month " +
                                "AND sales_per_month.year = EXCLUDED.year",
                        (JdbcStatementBuilder<SalesPerMonth>) (preparedStatement, salesPerMonth) -> {
                            preparedStatement.setInt(1, salesPerMonth.getMonth());
                            preparedStatement.setInt(2, salesPerMonth.getYear());
                            preparedStatement.setDouble(3, salesPerMonth.getTotalSales());
                        },
                        executionOptions,
                        connectionOptions
                )).name("Insert data into sales_per_month table");

        // Insert data into elastic search
        transactionDataStream.sinkTo(
                new Elasticsearch7SinkBuilder<Transaction>()
                        .setHosts(new HttpHost("localhost", 9200, "http"))
                        .setEmitter((transaction, runtimeContext, requestIndexer) -> {
                            String json = convertTransactionToJson(transaction);
                            IndexRequest indexRequest = Requests.indexRequest()
                                    .index("transactions")
                                    .id(transaction.getTransactionId())
                                    .source(json, XContentType.JSON);
                            requestIndexer.add(indexRequest);
                        })
                        .build()
        ).name("Feed data into elastic search");

        env.execute("Flink Ecommerce Realtime Streaming Job");
    }
}
