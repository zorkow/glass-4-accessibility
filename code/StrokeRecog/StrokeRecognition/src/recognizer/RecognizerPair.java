package recognizer;

import ink.forms.recognizer.RecClass;
import ink.forms.recognizer.Recognizer;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Behrang Sabeghi
 */
public final class RecognizerPair implements Serializable {


	private static final long serialVersionUID = 1L;
	private final double distance;
	private final RecClass rc;

	public RecognizerPair(double distance, RecClass cls) {
		this.distance = distance;
		rc = cls;
	}

	public double distance() {
		return distance;
	}

	public RecClass recClass() {
    	return rc;
	}

	@Override
	public String toString() {
		return "Recognition: " + rc.toString() + ", Distance: " + distance;
	}

	public static List<RecognizerPair> getRecognizerPair(List<Recognizer.Pair> rp) {
		ArrayList<RecognizerPair> rps = new ArrayList<>();
		for (Recognizer.Pair rp1 : rp) {
			rps.add(new RecognizerPair(rp1.distance(), rp1.recClass()));
		}
		return rps;
	}
}
