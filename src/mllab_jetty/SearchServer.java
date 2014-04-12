package mllab_jetty;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Enumeration;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;

public class SearchServer extends AbstractHandler
{
	private QueryProcessor qp = null;
	@SuppressWarnings("unused")
	private String page = "";
//	private int maxHits = 100;

	public SearchServer(QueryProcessor qp) throws Exception {
		this.qp = qp;
		page = readFile("meta/search.html");
	}
	public String readFile(String filePath) throws IOException {
		String content = "";
		try {
			BufferedReader br = new BufferedReader(new FileReader(filePath));
			for (String line = ""; (line=br.readLine()) != null; ) {
				content += line + "\n";
			}
			br.close();
		} catch (Exception e) {
			System.out.println("readFile--"+e);
			content = null;
		}
		return content;
	}
    public void handle(
					   String target,
                       Request baseRequest,
                       HttpServletRequest request,
                       HttpServletResponse response)
        throws IOException, ServletException
    {
		if (baseRequest.getMethod().equals("GET")) {
			handleGET(
					   target,
                       baseRequest,
                       request,
                       response);
		} else if (baseRequest.getMethod().equals("POST")) {
			handlePOST(
					   target,
                       baseRequest,
                       request,
                       response);
		}
	}
	public void handleGET(
					   String target,
                       Request baseRequest,
                       HttpServletRequest request,
                       HttpServletResponse response)
        throws IOException, ServletException
	{
		//System.out.println("in GET handler");//: target=" + target);

		if (target.startsWith("/favicon.ico")){
			//printRequest(baseRequest);
			response.setStatus(HttpServletResponse.SC_OK);
			response.setContentType("image/x-icon");
		    baseRequest.setHandled(true);
			ServletOutputStream sos = response.getOutputStream();
			FileInputStream fis = new FileInputStream("meta/favicon.ico");
			byte[] buffer = new byte[1000];
			while(fis.read(buffer) != -1) {
				sos.write(buffer);
			}
			fis.close();
			sos.flush();
			//System.out.println("sending favicon.ico");
			return;
		} else if (target.startsWith("/pramukhime")){
			//printRequest(baseRequest);
			target = "meta" + target;
			String output = readFile(target);
		    response.setContentType("text/javascript;charset=utf-8");
		    response.setStatus(HttpServletResponse.SC_OK);
		    baseRequest.setHandled(true);
			PrintWriter writer = response.getWriter();
			writer.print(output);
		} else {
			printRequest(baseRequest);
			String output = readFile(target);
			if (output != null) {
				output = "<textarea rows=50 cols=180>\n" + output + "\n</textarea>";
		    	response.setContentType("text/html;charset=utf-8");
				//output = output;
		    	//response.setContentType("text/plain;charset=utf-8");
			} else {
				//output = page;
				output = readFile("meta/search.html");
		    	response.setContentType("text/html;charset=utf-8");
			}
		    //response.setContentType("text/html;charset=utf-8");
		    response.setStatus(HttpServletResponse.SC_OK);
		    baseRequest.setHandled(true);
			PrintWriter writer = response.getWriter();
			writer.print(output);
		}
	}
	public void handlePOST(
					   String target,
                       Request baseRequest,
                       HttpServletRequest request,
                       HttpServletResponse response)
        throws IOException, ServletException
	{
		System.out.println("in POST handler");// + target);
		printRequest(baseRequest);
		@SuppressWarnings("rawtypes")
		Enumeration names = baseRequest.getParameterNames();
		String queryterm = "";
		while(names.hasMoreElements()) {
			String name = (String) names.nextElement();
			if (name.equals("query")) {
				for (String value : baseRequest.getParameterValues(name)) {
					queryterm += value;
					break;
				}
				break;
			}
		}
		System.out.println("Query=" + queryterm);
		if (queryterm.equals("")) {
			return;
		}

        response.setContentType("text/html;charset=utf-8");
        response.setStatus(HttpServletResponse.SC_OK);
        baseRequest.setHandled(true);
		PrintWriter writer = response.getWriter();
// 		writer.println("<h1>Results</h1>\n");
		try {
			if (qp != null) {
				System.out.println("Searching " + queryterm);
// 				Query query = li.getQuery(queryterm);
// 				TopDocs results = li.search(query);
// 				writer.println(formatResults(query, results));
				writer.println(qp.search(queryterm));
			}
		} catch (Exception e) {
			System.out.println(e);
		}
    }
/*	public String formatResults(Query query, TopDocs results) throws Exception {
		String output = "";
		output += "<h3>Searching for: " + query.toString() + "</h3>\n";
		ScoreDoc[] hits = results.scoreDocs;
		int numTotalHits = results.totalHits;
		output += "<small>" + numTotalHits + " matching documents.</small><br>\n";
		int start = 0;
		int end = Math.min(numTotalHits, maxHits);
		output += "<ol>\n";
		for (int i = start; i < end; i++) {
			Document doc = li.doc(hits[i].doc);
			String title = doc.get("title");
			if (title == null) {
				title = "UNTITLED";
			}
			String path = doc.get("path");
			if (path != null) {
				output += formatLine(title, path, hits[i].score, true);
			} else {
				output += formatLine(title, path, hits[i].score, false);
			}
		}
		output += "</ol>\n";
		return output;
	}*/
/*	private String formatLine(String title, String path, double score, boolean hyperlink) {
		String line = "";
		if (hyperlink) {
			line = "<li><a href=" + path + ">" + title + "</a>&nbsp;<small><small>(" + score + ")</small></small></li>\n";
		} else {
			line = "<li>" + title + "</a>&nbsp;<small>(" + score + ")<small><li>\n";
		}
		return line;
	}*/
	private void printRequest(Request req) {
		String dump = "";
		dump += "BEGIN REQUEST-----\n";
		dump += req.toString() + "\n";
/*		dump += req.getCharacterEncoding() + "\n"; */
		dump += req.getContentType() + "\n";
/*		dump += req.getContextPath() + "\n";
		dump += req.getHeader("") + "\n";
		dump += req.getContextPath() + "\n";
		dump += req.getLocalAddr() + "\n";
		dump += req.getLocalName() + "\n";
		dump += req.getMethod() + "\n"; */
		dump += "PARAMS ";
		@SuppressWarnings("rawtypes")
		Enumeration names = req.getParameterNames();
		while(names.hasMoreElements()) {
			String name = (String) names.nextElement();
			dump += name + "=";
			for (String value : req.getParameterValues(name)) {
				dump += value + ",";
			}
			dump += ";";
		}
		dump += "\n";
		dump += "pathinfo:" + req.getPathInfo() + "\n";
/*		dump += req.getPathTranslated() + "\n";
		dump += req.getProtocol() + "\n";
		dump += req.getQueryEncoding() + "\n";
		dump += req.getQueryString() + "\n";
		dump += req.getRequestURI() + "\n";
		dump += req.getRequestURL().toString() + "\n";
		dump += req.getServletName() + "\n";
		dump += req.getServletPath() + "\n"; */
		dump += "\n------END REQUEST";
		System.out.println(dump);
	}
    public static void main(String[] args) throws Exception
    {
		// ~/Desktop/files/work/mllab/expt/lucene/index1 ~/Desktop/files/work/mllab/expt/lucene/data/temp.infer ~/Desktop/files/work/mllab/expt/lucene/data/temp.maet TOPIC 8081
		String cmd = args[0];
		int i = 1;
		if (cmd.equals("wiki")) {
			String indexPath = args[i++];
			String inferencer = args[i++];
			String instanceFile = args[i++];
			String indexType = args[i++];
			String port = args[i++];
			QueryProcessor qp = new LuceneIndex(indexPath, inferencer, instanceFile, indexType);
			Server server = new Server(Integer.parseInt(port));
			server.setHandler(new SearchServer(qp));
			server.start();
			server.join();
		} else if (cmd.equals("gita")) {
//			String indexPath = args[i++];
			String port = args[i++];
//			QueryProcessor qp = new GitaParser(indexPath);
			Server server = new Server(Integer.parseInt(port));
//			server.setHandler(new SearchServer(qp));
			server.start();
			server.join();
		}
    }
}

