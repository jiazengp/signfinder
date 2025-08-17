package net.signfinder.test;

/**
 * Represents the result of a test case execution.
 * Tracks success/failure status and provides aggregation capabilities.
 */
public class TestResult
{
	private final boolean passed;
	private final String testName;
	private final String message;
	private int testsRun = 1;
	private int testsPassed = 0;
	private int testsFailed = 0;
	
	private TestResult(boolean passed, String testName, String message)
	{
		this.passed = passed;
		this.testName = testName;
		this.message = message;
		this.testsPassed = passed ? 1 : 0;
		this.testsFailed = passed ? 0 : 1;
	}
	
	/**
	 * Create a successful test result.
	 */
	public static TestResult passed(String testName)
	{
		return new TestResult(true, testName, "Test passed");
	}
	
	/**
	 * Create a failed test result.
	 */
	public static TestResult failed(String message)
	{
		return new TestResult(false, "Unknown", message);
	}
	
	/**
	 * Create an empty test result for aggregation.
	 */
	public static TestResult empty()
	{
		TestResult result = new TestResult(true, "Aggregate", "");
		result.testsRun = 0;
		result.testsPassed = 0;
		result.testsFailed = 0;
		return result;
	}
	
	/**
	 * Add another test result to this aggregate result.
	 */
	public TestResult add(TestResult other)
	{
		TestResult combined = new TestResult(true, "Aggregate", "");
		combined.testsRun = this.testsRun + other.testsRun;
		combined.testsPassed = this.testsPassed + other.testsPassed;
		combined.testsFailed = this.testsFailed + other.testsFailed;
		return combined;
	}
	
	public boolean isPassed()
	{
		return passed;
	}
	
	public String getTestName()
	{
		return testName;
	}
	
	public String getMessage()
	{
		return message;
	}
	
	public int getTestsRun()
	{
		return testsRun;
	}
	
	public int getTestsPassed()
	{
		return testsPassed;
	}
	
	public int getTestsFailed()
	{
		return testsFailed;
	}
	
	public double getSuccessRate()
	{
		return testsRun > 0 ? (double)testsPassed / testsRun * 100.0 : 0.0;
	}
	
	@Override
	public String toString()
	{
		return String.format(
			"TestResult{run=%d, passed=%d, failed=%d, success=%.1f%%}",
			testsRun, testsPassed, testsFailed, getSuccessRate());
	}
}
