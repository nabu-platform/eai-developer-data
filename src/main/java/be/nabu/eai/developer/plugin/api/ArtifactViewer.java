package be.nabu.eai.developer.plugin.api;

import java.util.ArrayList;
import java.util.List;

import be.nabu.eai.repository.api.Entry;
import be.nabu.libs.artifacts.api.Artifact;
import javafx.scene.Node;
import javafx.scene.control.Button;

public interface ArtifactViewer<T extends Artifact> {
	public String getName();
	public String getSectionGraphic();
	public Class<T> getArtifactClass();
	public void create();
	public Node draw(Entry projectEntry, T artifact);
	// by default we limit to artifacts within the project
	public default boolean allow(Entry projectEntry, String artifactId) {
		return artifactId.startsWith(projectEntry.getId() + ".");
	}
	public default String getArtifactGraphic() {
		return null;
	}
	public default List<Button> getButtons(Entry project, T artifact) {
		return new ArrayList<Button>();
	}
}
