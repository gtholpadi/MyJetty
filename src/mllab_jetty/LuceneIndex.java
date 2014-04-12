package mllab_jetty;
import java.io.File;

import mllab_clinss.Clinss;
import mllab_lucene.StringArrayIterator;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.FuzzyQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;

import cc.mallet.pipe.Pipe;
import cc.mallet.topics.TopicInferencer;
import cc.mallet.types.InstanceList;

public class LuceneIndex implements QueryProcessor {
//	private String field = "contents";
	private IndexReader reader = null;
	private IndexSearcher searcher = null;
//	private Analyzer analyzer;
//	private QueryParser parser;
	public int maxHits = 100;
	//for query generation
//	private String indexType;
	private TopicInferencer ti;
	private Pipe instancePipe;
	private Clinss clinss;

	public LuceneIndex(String indexPath, String inferencer, String instancefile, String indexType) throws Exception {
		reader = DirectoryReader.open(FSDirectory.open(new File(indexPath)));
		searcher = new IndexSearcher(reader);
//		analyzer = new StandardAnalyzer(Version.LUCENE_44);
//		parser = new QueryParser(Version.LUCENE_44, field, analyzer);
		//for query generation
		clinss = new Clinss(inferencer, instancefile, indexType);
//		this.indexType = indexType;
		if (indexType.equals("TOPIC")) {
			System.out.println("QWEQWE");
			ti = TopicInferencer.read(new File(inferencer));
			instancePipe = InstanceList.load(new File(instancefile)).getPipe();
		}
	}
	public Query getQuery(String line) throws Exception {
		return clinss.getQuery(line);
/*		if (indexType.equals("TOPIC")) {
			return getTopicQuery(line);
		} else {
			//Query query = parser.parse(line);
			//return getExactQuery(line);
			return getFuzzyQuery(line);
		}
*/
	}
	public Query getExactQuery(String line) throws Exception {
		String [] terms = line.trim().split("\\s+");
		BooleanQuery bq = new BooleanQuery();
		for (String term : terms) {
			//TermQuery qpat = new TermQuery(new Term("path", term));
			TermQuery qtit = new TermQuery(new Term("title", term));
			TermQuery qcon = new TermQuery(new Term("contents", term));

			//bq.add(qpat, BooleanClause.Occur.SHOULD);
			bq.add(qtit, BooleanClause.Occur.SHOULD);
			bq.add(qcon, BooleanClause.Occur.SHOULD);
		}
		return bq;
	}
	public Query getFuzzyQuery(String line) throws Exception {
		String [] terms = line.trim().split("\\s+");
		BooleanQuery bq = new BooleanQuery();
		for (String term : terms) {
			//TermQuery qpat = new TermQuery(new Term("path", term));
			FuzzyQuery qtit = new FuzzyQuery(new Term("title", term), 1);
			FuzzyQuery qcon = new FuzzyQuery(new Term("contents", term), 1);

			//bq.add(qpat, BooleanClause.Occur.SHOULD);
			bq.add(qtit, BooleanClause.Occur.SHOULD);
			bq.add(qcon, BooleanClause.Occur.SHOULD);
		}
		return bq;
	}
	public Query getTopicQuery(String line) throws Exception {
		InstanceList instances = new InstanceList(instancePipe);
		String[] strarr = {line};
		instances.addThruPipe(new StringArrayIterator(strarr));
		double[] dist = ti.getSampledDistribution(instances.get(0), 100, 10, 10);
		String topics = "";
		for (int i=0; i<dist.length; i++) {
			//System.out.print(i+" "+dist[i]+", ");
			if (dist[i]>=0.25) {
				topics += i + " ";
			}
		}
		return getExactQuery(topics);
	}
	public TopDocs search(Query query) throws Exception {
		return searcher.search(query, maxHits);
	}
	public String search(String line) throws Exception {
		Query query = getQuery(line);
		TopDocs results = searcher.search(query, maxHits);
		return formatResults(query, results);
	}
	public Document doc(int doc) throws Exception {
		return searcher.doc(doc);
	}
	public String formatResults(Query query, TopDocs results) throws Exception {
		StringBuilder output = new StringBuilder();
		output.append("<h1>Results</h1>\n");
		output.append("<h3>Searching for: " + query.toString() + "</h3>\n");
		ScoreDoc[] hits = results.scoreDocs;
		int numTotalHits = results.totalHits;
		output.append("<small>" + numTotalHits + " matching documents.</small><br>\n");
		int start = 0;
		int end = Math.min(numTotalHits, maxHits);
		output.append("<ol>\n");
		for (int i = start; i < end; i++) {
			Document doc = searcher.doc(hits[i].doc);
			String title = doc.get("title");
			if (title == null) {
				title = "UNTITLED";
			}
			String path = doc.get("path");
			if (path != null) {
				output.append(formatLine(title, path, hits[i].score, true));
			} else {
				output.append(formatLine(title, path, hits[i].score, false));
			}
		}
		output.append("</ol>\n");
		return output.toString();
	}
	private String formatLine(String title, String path, double score, boolean hyperlink) {
		String line;
		if (hyperlink) {
			line = "<li><a href=" + path + ">" + title + "</a>&nbsp;<small><small>(" + score + ")</small></small></li>\n";
		} else {
			line = "<li>" + title + "&nbsp;<small><small>(" + score + ")</small></small></li>\n";
		}
		return line;
	}
}
