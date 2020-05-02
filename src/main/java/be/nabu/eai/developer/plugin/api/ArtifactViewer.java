package be.nabu.eai.developer.plugin.api;

import java.util.ArrayList;
import java.util.List;

import be.nabu.libs.artifacts.api.Artifact;
import javafx.scene.Node;
import javafx.scene.control.Button;

public interface ArtifactViewer<T extends Artifact> {
	public String getName();
	public String getSectionGraphic();
	public Class<T> getArtifactClass();
	public void create();
	public Node draw(String project, T artifact);
	// by default we limit to artifacts within the project
	public default boolean allow(String project, String artifactId) {
		return artifactId.startsWith(project + ".");
	}
	public default String getArtifactGraphic() {
		return null;
	}
	public default List<Button> getButtons(String project, T artifact) {
		return new ArrayList<Button>();
	}
}
