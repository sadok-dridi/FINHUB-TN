import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class TestHashFormat {
    public static void main(String[] args) {
        // Test Date Format
        LocalDateTime t1 = LocalDateTime.of(2023, 10, 27, 10, 0, 0); // Zero seconds
        LocalDateTime t2 = LocalDateTime.of(2023, 10, 27, 10, 0, 5); // Non-zero seconds

        System.out.println("T1 (00 sec): " + t1.truncatedTo(ChronoUnit.SECONDS).toString());
        System.out.println("T2 (05 sec): " + t2.truncatedTo(ChronoUnit.SECONDS).toString());

        // Test Decimal Format
        BigDecimal d1 = new BigDecimal("100.5");
        BigDecimal d2 = new BigDecimal("100.00");

        System.out.println("D1 (100.5): " + d1.setScale(3, RoundingMode.HALF_UP).toString());
        System.out.println("D2 (100.00): " + d2.setScale(3, RoundingMode.HALF_UP).toString());
    }
}
