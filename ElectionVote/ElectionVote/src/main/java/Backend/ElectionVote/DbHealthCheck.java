//package Backend.ElectionVote;
//
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.boot.CommandLineRunner;
//import org.springframework.jdbc.core.JdbcTemplate;
//import org.springframework.stereotype.Component;
//
//@Component
//public class DbHealthCheck implements CommandLineRunner {
//    private static final Logger log = LoggerFactory.getLogger(DbHealthCheck.class);
//    private final JdbcTemplate jdbc;
//
//    public DbHealthCheck(JdbcTemplate jdbc) { this.jdbc = jdbc; }
//
//    @Override
//    public void run(String... args) {
//        Integer one = jdbc.queryForObject("select 1", Integer.class);
//        log.info("✅ DB check OK: select 1 -> {}", one);
//    }
//}