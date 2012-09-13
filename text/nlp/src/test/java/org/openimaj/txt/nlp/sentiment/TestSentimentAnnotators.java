package org.openimaj.txt.nlp.sentiment;

/*import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.openimaj.feature.DoubleFV;
import org.openimaj.feature.DoubleFVComparison;
import org.openimaj.io.FileUtils;
import org.openimaj.ml.annotation.AnnotatedObject;
import org.openimaj.ml.annotation.ScoredAnnotation;
import org.openimaj.ml.annotation.basic.KNNAnnotator;
import org.openimaj.ml.annotation.bayes.NaiveBayesAnnotator;
import org.openimaj.text.nlp.EntityTweetTokeniser;
import org.openimaj.text.nlp.TweetTokeniserException;
import org.openimaj.text.nlp.sentiment.model.classifier.GeneralSentimentFeatureExtractor;
import org.openimaj.text.nlp.sentiment.type.BipolarSentiment;
import org.openimaj.text.nlp.sentiment.type.WeightedBipolarSentiment;*/

public class TestSentimentAnnotators {
	//Need to rebuild these after extensive refactoring.

	/*private String negSource = "/org/openimaj/text/nlp/sentiment/imdbreview/neg.txt";
	private String posSource = "/org/openimaj/text/nlp/sentiment/imdbreview/pos.txt";
	private float TRAIN_PROP = 0.9f;
	private List<List<String>> rawTrainingTokens;
	private List<AnnotatedObject<List<String>, WeightedBipolarSentiment>> trainList;
	private List<AnnotatedObject<List<String>, WeightedBipolarSentiment>> negExamples;
	private List<AnnotatedObject<List<String>, WeightedBipolarSentiment>> posExamples;
	private int nTrainNeg;
	private int nTrainPos;
	private int knn = 3;
	private DoubleFVComparison comparison = DoubleFVComparison.BHATTACHARYYA;
	private int knnLim = 10;
	private double acceptablePercentCorrect = 0.7;

	*//**
	 * Create the model, prepare test statements
	 *//*
	@Before
	public void setup() {
		System.out.println("Setting up...");
		try {
			negExamples = loadIMDBSource(negSource,
					new WeightedBipolarSentiment(0f, 1.0f, 0f));
			posExamples = loadIMDBSource(posSource,
					new WeightedBipolarSentiment(1.0f, 0f, 0f));
		} catch (IOException e) {
			e.printStackTrace();
		}

		nTrainNeg = (int) (negExamples.size() * TRAIN_PROP);
		nTrainPos = (int) (posExamples.size() * TRAIN_PROP);
		nTrainPos = nTrainNeg = Math.min(nTrainNeg, nTrainPos);

		// Build training set.
		trainList = new ArrayList<AnnotatedObject<List<String>, WeightedBipolarSentiment>>();
		trainList.addAll(negExamples.subList(0, nTrainNeg));
		trainList.addAll(posExamples.subList(0, nTrainPos));

		rawTrainingTokens = new ArrayList<List<String>>();
		try {
			addRawTo(rawTrainingTokens, negSource, nTrainNeg);
			addRawTo(rawTrainingTokens, posSource, nTrainPos);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Test
	public void dataLengths() {
		System.out.println("NEGATIVE");
		doStatsOn(negExamples);
		System.out.println("POSITIVE");
		doStatsOn(posExamples);
	}

	private void doStatsOn(
			List<AnnotatedObject<List<String>, WeightedBipolarSentiment>> set) {
		int negWords = 0;
		int minNeg = 100000;
		int maxNeg = 0;
		int negs = 0;
		for (AnnotatedObject<List<String>, WeightedBipolarSentiment> instance : set) {
			negWords += instance.object.size();
			negs++;
			if (instance.object.size() < minNeg)
				minNeg = instance.object.size();
			if (instance.object.size() > maxNeg)
				maxNeg = instance.object.size();
		}
		int navg = negWords / negs;
		int totalDev = 0;
		for (AnnotatedObject<List<String>, WeightedBipolarSentiment> instance : set) {
			int size = instance.object.size();
			int dev = Math.abs(size - navg);
			totalDev += dev;
		}
		int sDev = totalDev / negs;
		System.out.println("Average: " + negWords / negs);
		System.out.println("Low outlier: " + minNeg);
		System.out.println("High outier: " + maxNeg);
		System.out.println("SDev: " + sDev);
	}

	@Test
	public void testIMDBReviewSandia() throws IOException {
		System.out.println("@TEST: Sandia");
		// Train
		GeneralSentimentFeatureExtractor fExtractor = new GeneralSentimentFeatureExtractor(
				rawTrainingTokens);
		NaiveBayesAnnotator<List<String>, WeightedBipolarSentiment, GeneralSentimentFeatureExtractor> nbAnnotator = new NaiveBayesAnnotator<List<String>, WeightedBipolarSentiment, GeneralSentimentFeatureExtractor>(
				fExtractor, NaiveBayesAnnotator.Mode.MAXIMUM_LIKELIHOOD);
		nbAnnotator.train(trainList);
		//Test
		double wrongCountN = 0;
		double rightCountN = 0;
		double wrongCountP = 0;
		double rightCountP = 0;
		for (AnnotatedObject<List<String>, WeightedBipolarSentiment> anno : negExamples
				.subList(nTrainNeg, negExamples.size())) {
			List<ScoredAnnotation<WeightedBipolarSentiment>> result = nbAnnotator
					.annotate(anno.getObject());
			if (result.get(0).annotation.bipolar() == BipolarSentiment.NEGATIVE)
				rightCountN++;
			else
				wrongCountN++;
		}
		for (AnnotatedObject<List<String>, WeightedBipolarSentiment> anno : posExamples
				.subList(nTrainNeg, negExamples.size())) {
			List<ScoredAnnotation<WeightedBipolarSentiment>> result = nbAnnotator
					.annotate(anno.getObject());
			if (result.get(0).annotation.bipolar() == BipolarSentiment.POSITIVE)
				rightCountP++;
			else
				wrongCountP++;
		}
		double correctPercN = rightCountN / (rightCountN + wrongCountN);
		double correctPercP = rightCountP / (rightCountP + wrongCountP);
		System.out.println("Percent Correct Neg: " + correctPercN);
		System.out.println("Percent Correct Pos: " + correctPercP);
		//assertTrue(correctPerc > acceptablePercentCorrect);
		System.out.println("Total: "+(correctPercN+correctPercP)/2); 
		System.out.println("Done");
	}

	@Test
	public void testIMDBReviewSandiaSingleToken() throws IOException {
		System.out.println("@TEST: SingleTokenSandia");
		GeneralSentimentFeatureExtractor fExtractor = new GeneralSentimentFeatureExtractor(
				rawTrainingTokens);
		NaiveBayesAnnotator<List<String>, WeightedBipolarSentiment, GeneralSentimentFeatureExtractor> nbAnnotator = new NaiveBayesAnnotator<List<String>, WeightedBipolarSentiment, GeneralSentimentFeatureExtractor>(
				fExtractor, NaiveBayesAnnotator.Mode.MAXIMUM_LIKELIHOOD);

		// Train
		nbAnnotator.train(trainList);

		// Test
		String[] negative = new String[] { "terrible", "bad", "disgusting" };
		String[] positive = new String[] { "great", "good", "wonderful" };
		for (String token : positive) {
			List<ScoredAnnotation<WeightedBipolarSentiment>> result = nbAnnotator
					.annotate(Arrays.asList(token.split(" ")));
			System.out.println(token);
			System.out.println(result.get(0).annotation.bipolar());
		}
		for (String token : negative) {
			List<ScoredAnnotation<WeightedBipolarSentiment>> result = nbAnnotator
					.annotate(Arrays.asList(token.split(" ")));
			System.out.println(token);
			System.out.println(result.get(0).annotation.bipolar());
		}

	}

	@Test
	public void testIMDBReviewKNN() throws IOException {
		System.out.println("@TEST: KNN");
		// Train
		GeneralSentimentFeatureExtractor fExtractor = new GeneralSentimentFeatureExtractor(
				rawTrainingTokens);
		KNNAnnotator<List<String>, WeightedBipolarSentiment, GeneralSentimentFeatureExtractor, DoubleFV> knnAnnotator = new KNNAnnotator<List<String>, WeightedBipolarSentiment, GeneralSentimentFeatureExtractor, DoubleFV>(
				fExtractor, comparison, knn);
		assertTrue(knnRun(knnAnnotator) > acceptablePercentCorrect);

		
		 * //Uncomment this to find the best KNN metric. DoubleFVComparison[]
		 * comporators = new DoubleFVComparison[]{
		 * DoubleFVComparison.BHATTACHARYYA, DoubleFVComparison.CHI_SQUARE,
		 * DoubleFVComparison.CORRELATION, DoubleFVComparison.EUCLIDEAN,
		 * DoubleFVComparison.HAMMING, DoubleFVComparison.INTERSECTION, };
		 * double topCorrect = 0; DoubleFVComparison topMetric = null;
		 * for(DoubleFVComparison comp : comporators){ for(int i = 1; i< knnLim;
		 * i++){ KNNAnnotator<List<String>, WeightedBipolarSentiment,
		 * GeneralSentimentFeatureExtractor, DoubleFV> knnAnnotator = new
		 * KNNAnnotator<List<String>, WeightedBipolarSentiment,
		 * GeneralSentimentFeatureExtractor, DoubleFV>( fExtractor, comp, i);
		 * double result=knnRun(knnAnnotator); if(result>topCorrect){ topCorrect
		 * = result; topMetric = comp; } } }
		 * System.out.println("Top KNN : "+topCorrect);
		 * System.out.println("Top Metric: "+topMetric);
		 

	}

	private double knnRun(
			KNNAnnotator<List<String>, WeightedBipolarSentiment, GeneralSentimentFeatureExtractor, DoubleFV> knnAnnotator) {

		knnAnnotator.train(trainList);

		double wrongCount = 0;
		double rightCount = 0;
		for (AnnotatedObject<List<String>, WeightedBipolarSentiment> anno : negExamples
				.subList(nTrainNeg, negExamples.size())) {
			List<ScoredAnnotation<WeightedBipolarSentiment>> result = knnAnnotator
					.annotate(anno.getObject());
			if (result.get(0).annotation.bipolar() == BipolarSentiment.NEGATIVE)
				rightCount++;
			else
				wrongCount++;
		}
		for (AnnotatedObject<List<String>, WeightedBipolarSentiment> anno : posExamples
				.subList(nTrainNeg, negExamples.size())) {
			List<ScoredAnnotation<WeightedBipolarSentiment>> result = knnAnnotator
					.annotate(anno.getObject());
			if (result.get(0).annotation.bipolar() == BipolarSentiment.POSITIVE)
				rightCount++;
			else
				wrongCount++;
		}
		double correctPerc = rightCount / (rightCount + wrongCount);
		System.out.println("Total Tests: " + (wrongCount + rightCount));
		System.out.println("Percent Correct: " + correctPerc);
		return correctPerc;
	}

	private List<String> tok(String line) {
		EntityTweetTokeniser t = null;
		try {
			t = new EntityTweetTokeniser(line);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (TweetTokeniserException e) {
			e.printStackTrace();
		}
		return t.getStringTokens();
	}

	private List<AnnotatedObject<List<String>, WeightedBipolarSentiment>> loadIMDBSource(
			String sourceList, WeightedBipolarSentiment sentiment)
			throws IOException {
		String[] sources = FileUtils.readlines(TestSentimentAnnotators.class
				.getResourceAsStream(sourceList));
		List<AnnotatedObject<List<String>, WeightedBipolarSentiment>> ret = new ArrayList<AnnotatedObject<List<String>, WeightedBipolarSentiment>>();
		int totalWords = 0;
		for (String source : sources) {
			List<String> wordList = tok(FileUtils
					.readall(TestSentimentAnnotators.class
							.getResourceAsStream(source)));
			totalWords += wordList.size();
			AnnotatedObject<List<String>, WeightedBipolarSentiment> wordsentimentpair = new AnnotatedObject(
					wordList, sentiment);
			ret.add(wordsentimentpair);
		}
		return ret;
	}

	private void addRawTo(List<List<String>> rawTrainingTokens,
			String sourceList, int limit) throws IOException {
		String[] sources = FileUtils.readlines(TestSentimentAnnotators.class
				.getResourceAsStream(sourceList));
		int count = 0;
		for (String source : sources) {
			count++;
			if (count == limit)
				break;
			List<String> wordList = Arrays.asList(FileUtils.readall(
					TestSentimentAnnotators.class.getResourceAsStream(source))
					.split("\\s+"));
			rawTrainingTokens.add(wordList);
		}
	}*/
}