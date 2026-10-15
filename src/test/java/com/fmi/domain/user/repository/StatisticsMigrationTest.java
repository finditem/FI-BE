package com.fmi.domain.user.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.*;
import java.sql.DriverManager;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.ClassPathResource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.*;

@Testcontainers
@DisplayName("통계 Flyway migration")
class StatisticsMigrationTest {
    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4");

    @TempDir
    Path migrations;

    @BeforeEach
    void setUp() throws Exception {
        Flyway.configure()
                .dataSource(mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword())
                .cleanDisabled(false)
                .load()
                .clean();
        try (var input =
                new ClassPathResource("db/migration/V16__create_user_and_post_statistics.sql").getInputStream()) {
            Files.copy(input, migrations.resolve("V16__create_user_and_post_statistics.sql"));
        }
    }

    @Test
    @DisplayName("V16 단독 적용은 빈 DB에 통계 테이블 두 개를 생성한다")
    void itCreatesBothTablesInEmptyDatabase() throws Exception {
        Flyway migration = Flyway.configure()
                .dataSource(mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword())
                .locations("filesystem:" + migrations)
                .load();
        assertThat(migration.migrate().migrationsExecuted).isEqualTo(1);
        migration.validate();
        try (var connection =
                        DriverManager.getConnection(mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword());
                var statement = connection.createStatement()) {
            try (var rows = statement.executeQuery("SELECT COUNT(*) FROM user_statistics")) {
                rows.next();
                assertThat(rows.getLong(1)).isZero();
            }
            try (var rows = statement.executeQuery("SELECT COUNT(*) FROM post_statistics")) {
                rows.next();
                assertThat(rows.getLong(1)).isZero();
            }
        }
        assertThat(migration.migrate().migrationsExecuted).isZero();
    }

    @Test
    @DisplayName("버전 15로 표시한 기존 스키마에 V16를 추가하고 기존 행을 보존한다")
    void itAddsTablesWithoutChangingExistingData() throws Exception {
        try (var connection =
                        DriverManager.getConnection(mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword());
                var statement = connection.createStatement()) {
            statement.execute("CREATE TABLE existing_data (id BIGINT PRIMARY KEY)");
            statement.execute("INSERT INTO existing_data VALUES (1)");
        }
        Flyway migration = Flyway.configure()
                .dataSource(mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword())
                .locations("filesystem:" + migrations)
                .baselineOnMigrate(true)
                .baselineVersion("15")
                .load();
        assertThat(migration.migrate().migrationsExecuted).isEqualTo(1);
        migration.validate();
        try (var connection =
                        DriverManager.getConnection(mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword());
                var statement = connection.createStatement();
                var rows = statement.executeQuery("SELECT COUNT(*) FROM existing_data")) {
            rows.next();
            assertThat(rows.getLong(1)).isEqualTo(1);
        }
    }
}
