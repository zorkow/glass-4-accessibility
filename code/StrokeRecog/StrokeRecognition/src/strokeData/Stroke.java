package strokeData;

import java.util.ArrayList;
import java.util.List;

public class Stroke {

	private List<SubStroke> points;
	
	public Stroke() {
		this.points = new ArrayList<SubStroke>();
	}
	
	public Stroke(List<SubStroke> points) {
		this.points = points;
	}
	
	public List<SubStroke> getPoints() {
		return points;
	}
	
	public void addPoints(List<SubStroke> newPoints) {
		points.addAll(newPoints);
	}
	
	public void setPoints(List<SubStroke> points) {
		this.points = points;
	}
	
	public void changePenState() {
		for(int i=0; i<points.size(); i++) {
			points.get(i).setPenDown(!points.get(i).isPenDown());
		}
	}
}
