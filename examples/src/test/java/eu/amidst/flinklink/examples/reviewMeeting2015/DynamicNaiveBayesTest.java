package eu.amidst.flinklink.examples.reviewMeeting2015;

import junit.framework.TestCase;
import org.junit.Test;

public class DynamicNaiveBayesTest extends TestCase  {
	@Test
	public void test() throws Exception {

		String args[] = {"3","500", "5", "true"};


		GenerateData.main(args);
		DynamicNaiveBayes.main(null);
	}
}