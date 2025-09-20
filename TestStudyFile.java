public class TestStudyFile {
    public void demonstrateBadPractices() {
        String text = null;
        if (text == null) {
            System.out.println("This should use a logger instead of System.out.println");
        }
        // Missing exception handling - this will cause division by zero
        int result = 10 / 0;
        
        // Another bad practice
        for (int i = 0; i < 10; i++) {
            System.out.println("Loop " + i);  // Should use logger
        }
    }
    
    public String getStringValue() {
        return null; // This could cause NullPointerException
    }
}
