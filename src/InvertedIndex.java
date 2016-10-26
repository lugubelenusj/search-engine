import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Creates and stores an inverted index in the form of TreeMap<String,
 * TreeMap<String, TreeSet<Integer>>>. The inverted index documents words and
 * the files that they are found in, as well as the position that specific word
 * can be found in said file.
 */
public class InvertedIndex {

	/**
	 * The inverted index data structure.
	 */
	private final TreeMap<String, TreeMap<String, TreeSet<Integer>>> index;

	/**
	 * The constructor. Instantiates a new index.
	 */
	public InvertedIndex() {
		index = new TreeMap<>();
	}

	/**
	 * Adds a word, it's file, and it's position to the index, after checking to
	 * make sure the word, file, or index is not already included. If the word
	 * is included and not current file, or the word and file but not the
	 * current position, then it will add said file or position accordingly.
	 * 
	 * @param word
	 *            the word to add to the index
	 * @param file
	 *            the file that the word is found in
	 * @param position
	 *            the position in the file where the word is found
	 */
	public void add(String word, String file, Integer position) {
		if (!index.containsKey(word)) {
			index.put(word, new TreeMap<>());
		}

		if (index.get(word).get(file) == null) {
			index.get(word).put(file, new TreeSet<>());
		}

		index.get(word).get(file).add(position);
	}

	/**
	 * Passes the index and a file for the JSONWriter class to use in order to
	 * print the index's data onto a file in JSON format.
	 * 
	 * @param output
	 *            the file that the JSONWriter will print index's data onto.
	 * @throws IOExceptions
	 */
	public void toJSON(Path output) throws IOException {
		JSONWriter.writeNestedObject(output, index);
	}

	/**
	 * Searches index for the exact word or words in the query, and puts that
	 * word's location in terms of file, count, and size into a SearchResult
	 * object, which is placed into an ArrayList and then returned.
	 * 
	 * @param query
	 *            an array of search queries or a query.
	 * @return a list of SearchResult objects.
	 */
	public ArrayList<SearchResult> exactSearch(String[] query) {
		ArrayList<SearchResult> list = new ArrayList<>();

		HashMap<String, SearchResult> map = new HashMap<>();

		// Goes through each word in this query.
		for (String word : query) {
			if (index.containsKey(word)) {

				for (String file : index.get(word).keySet()) {
					int count = index.get(word).get(file).size();
					int firstPosition = index.get(word).get(file).first();

					// If the file exists, updates the SearchResult, else adds a
					// new SearchResult to the map.
					if (map.containsKey(file)) {
						map.get(file).addCount(count);
						if (firstPosition < map.get(file).getFirstPosition()) {
							map.get(file).setFirstPosition(firstPosition);
						}
					} else {
						map.put(file, new SearchResult(count, firstPosition, file));
					}
				}
			}
		}

		// Puts all of the maps SearchResults into a list.
		list.addAll(map.values());
		Collections.sort(list);
		return list;
	}

	/**
	 * Searches index for a word or words that start with the prefix or prefixes
	 * given in the query, and puts those word's or words' location in terms of
	 * file, count, and size into a SearchResult object, which is placed into an
	 * ArrayList and then returned.
	 * 
	 * @param query
	 *            an array of search queries or a query.
	 * @return a list of SearchResult objects.
	 */
	public ArrayList<SearchResult> partialSearch(String[] query) {
		ArrayList<SearchResult> list = new ArrayList<>();

		HashMap<String, SearchResult> map = new HashMap<>();

		// Goes through each word in this query.
		for (String prefix : query) {

			// Goes through each key in the tailMap of the index.
			for (String indexWord : index.tailMap(prefix).keySet()) {

				if (!indexWord.startsWith(prefix)) {
					break;
				}

				// Goes through each file in this key.
				for (String file : index.get(indexWord).keySet()) {
					int count = index.get(indexWord).get(file).size();
					int firstPosition = index.get(indexWord).get(file).first();

					// If the file exists, updates the SearchResult, else adds a
					// new SearchResult to the map.
					if (map.containsKey(file)) {
						map.get(file).addCount(count);
						if (firstPosition < map.get(file).getFirstPosition()) {
							map.get(file).setFirstPosition(firstPosition);
						}
					} else {
						map.put(file, new SearchResult(count, firstPosition, file));
					}
				}
			}
		}

		// Puts all of the maps SearchResults into a list.
		list.addAll(map.values());
		Collections.sort(list);
		return list;
	}
}