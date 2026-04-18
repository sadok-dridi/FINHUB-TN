import java.sql.Timestamp;
import java.time.LocalDateTime;

public class TestJava {
    public static void main(String[] args) {
        LocalDateTime dt = LocalDateTime.of(2026, 4, 4, 20, 43, 33);
        System.out.println("Timestamp: " + Timestamp.valueOf(dt).toString());
        
        LocalDateTime dt2 = LocalDateTime.of(2026, 4, 4, 20, 43, 0);
        System.out.println("LocalDateTime: " + dt2.toString());
    }
}
