import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringEscapeUtils;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;

public class SearchEngineServer {

	public final int port;
	private final InvertedIndex index;

	private final String googleLogo;
	private final String twoPointZeroLogo;
	private boolean incognito;

	public SearchEngineServer(int port, InvertedIndex index) {
		this.port = port;
		this.index = index;
		this.googleLogo = "https://www.google.com/images/branding/googlelogo/1x/googlelogo_color_272x92dp.png";
		this.twoPointZeroLogo = "https://smashingboxes.com/media/W1siZiIsIjIwMTUvMTAvMjAvMTAvNDEvNDgvOTE5L2FuZ3VsYXJfMi4wLnBuZyJdXQ/angular%202.0.png?sha=c182c65bfad4aa24";
		this.incognito = false;
	}

	public void startUp() throws Exception {
		Server server = new Server(port);

		ServletHandler handler = new ServletHandler();
		handler.addServletWithMapping(new ServletHolder(new SearchEngineServlet()), "/");
		handler.addServletWithMapping(new ServletHolder(new SearchHistoryServlet()), "/searchHistory");

		server.setHandler(handler);
		server.start();
		server.join();
	}

	@SuppressWarnings("serial")
	private class SearchEngineServlet extends HttpServlet {
		private static final String TITLE = "Google 2.0";
		private ArrayList<SearchResult> results;

		public SearchEngineServlet() {
			super();
			results = new ArrayList<>();
		}

		@Override
		protected void doGet(HttpServletRequest request, HttpServletResponse response)
				throws ServletException, IOException {

			response.setContentType("text/html");
			response.setStatus(HttpServletResponse.SC_OK);

			printForm(request, response);
			PrintWriter out = response.getWriter();

			if (request.getParameter("dontTrack") != null) {
				incognito = incognito == false ? true : false;
			}
			out.printf("<p><font color = \"white\">Incognito mode: %s</font></p>", incognito);

			if (request.getParameter("query") != null) {
				String query = request.getParameter("query");
				query = StringEscapeUtils.escapeHtml4(query);

				InvertedIndexBuilderInterface.clean(query);
				String[] words = query.split("\\s+");
				Arrays.sort(words);

				results = index.partialSearch(words);

				// Keep in mind multiple threads may access at once
				for (SearchResult result : results) {
					out.printf("<p><center><a href=\"%s\">%s</a></center></p>%n%n", result.getPath(), result.getPath());
					out.printf("<p><center></center></p>");
				}

				if (incognito == false) {
					response.addCookie(new Cookie(query, CookieBaseServlet.getShortDate()));
				}
			}

			out.printf("%n</body>%n");
			out.printf("</html>%n");

			response.setStatus(HttpServletResponse.SC_OK);
		}

		private void printForm(HttpServletRequest request, HttpServletResponse response) throws IOException {

			PrintWriter out = response.getWriter();
			out.printf("<html>%n%n");
			out.printf("<head>");
			out.printf("<title>%s</title>%n", TITLE);
			out.printf("<style> body { background-color:#000000; } </style>");
			out.printf("</head>");
			out.printf("<body>%n");

			out.printf("<h1><center><img src=\"%s\"/>%n", googleLogo);
			out.printf("<img src=\"%s\" height=\"128\" width=\"128\" />%n</center></h1>", twoPointZeroLogo);

			out.printf("<form method=\"get\" action=\"%s\">%n", request.getServletPath());
			out.printf("<p><center><input type=\"text\" name=\"query\" size=\"60\" maxlength=\"100\"/></center></p>%n");
			out.printf("<p><center><input type=\"submit\" value=\"Search\"></center></p>\n%n");
			out.printf("</form>%n");

			out.printf("<form method=\"get\" action=\"%s\">%n", "searchHistory");
			out.printf("\t<input type=\"submit\" value=\"Search History\">%n");
			out.printf("</form>%n");

			out.printf("<form method=\"get\" action=\"%s\">%n", request.getRequestURI());
			out.printf("\t<input type=\"submit\" name=\"dontTrack\" value=\"Go Incognito\" >%n");
			out.printf("</form>%n");

			out.printf("<form method=\"get\" action=\"%s\" name=\"exact\">%n", request.getRequestURI());
			out.printf("\t<input type=\"submit\" value=\"exact/partial\">%n");
			out.printf("</form>%n");

			out.printf("</body>%n");
			out.printf("</html>%n");
		}
	}
}