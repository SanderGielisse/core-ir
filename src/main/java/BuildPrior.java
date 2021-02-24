import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import lemurproject.indri.ParsedDocument;
import lemurproject.indri.QueryEnvironment;

public class BuildPrior {

	private static final int CHUNK_SIZE = 2048;

	// calculate prior P(D), the probability of the document D
	public static void main(String[] strings) throws Exception {
		// go through the documents and match their id with a prior probability
		final QueryEnvironment env = new QueryEnvironment();
		// by default, Indri uses a query likelihood function with Dirichlet prior
		// smoothing to weight terms
		env.setStopwords(BuildIndex.getStopWords());
		System.out.println("Loading indexes...");
		env.addIndex("/home/coreir/lm_model/msmarco.idx");
		System.out.println("Environment loaded, found " + env.documentCount() + " document(s).");

		final List<Integer> all = new ArrayList<>();
		for (int i = 1; i <= env.documentCount(); i++)
			all.add(i);
		final List<List<Integer>> batches = partition(all, CHUNK_SIZE);

		try (PrintWriter writerPrior = new PrintWriter(new File("./prior_values.dat"))) {
			for (int batchId = 0; batchId < batches.size(); batchId++) {
				final List<Integer> batch = batches.get(batchId);
				System.out.println("Processing batch " + (batchId + 1) + "/" + batches.size() + "...");
				final ParsedDocument[] pds = env.documents(toPrimitive(batch));
				for (int i = 0; i < pds.length; i++) {
					final int id = batch.get(i);
					final ParsedDocument doc = pds[i];
					// calculate the prior
					final double prior = calculatePrior(doc);
					writerPrior.println(id + " " + Math.log(prior));
				}
			}
		}

		for (ParsedDocument s : env.documents(new int[] { 1, 2, 3, 4, 5, 6 })) {
			System.out.println(s.metadata);
		}
	}

	private static double calculatePrior(ParsedDocument doc) {
		// simple prior, document length
		return doc.content.length();
	}

	private static int[] toPrimitive(List<Integer> batch) {
		final int[] res = new int[batch.size()];
		for (int i = 0; i < res.length; i++)
			res[i] = batch.get(i);
		return res;
	}

	private static List<List<Integer>> partition(Collection<Integer> members, int maxSize) {
		final List<List<Integer>> res = new ArrayList<>();
		List<Integer> internal = new ArrayList<>();

		for (Integer member : members) {
			internal.add(member);

			if (internal.size() == maxSize) {
				res.add(internal);
				internal = new ArrayList<>();
			}
		}
		if (!internal.isEmpty())
			res.add(internal);
		return res;
	}
}
