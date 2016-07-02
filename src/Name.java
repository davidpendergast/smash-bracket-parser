import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author dpendergast
 */
public class Name {
	
	public String name;
	public List<String> aliases = new ArrayList<String>();
	
	public Name() {
		
	}
	
	public Name(String name) {
		this.name = name;
	}
	
	public Name(String name, List<String> aliases) {
		this.name = name;
		this.aliases.addAll(aliases);	
	}
	
	public boolean equals(Object o) {
		if (o instanceof String) {
			String oStr = (String) o;
			return name.equalsIgnoreCase(oStr) 
					|| aliases.stream().filter(s -> s.equalsIgnoreCase(oStr)).collect(Collectors.toList()).size() > 0;
		} else if (o instanceof Name) {
			return ((Name)o).name.equals(name);
		}
		return false;
	}
	
	public int hashCode() {
		return name.hashCode();
	}
	
	public String toString() {
		return name;
	}

}
