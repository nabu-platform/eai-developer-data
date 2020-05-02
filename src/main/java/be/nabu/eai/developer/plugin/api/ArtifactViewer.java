package be.nabu.eai.developer.plugin.api;

import be.nabu.libs.artifacts.api.Artifact;
import javafx.scene.Node;

public interface ArtifactViewer<T extends Artifact> {
	public String getName();
	public String getGraphic();
	public Class<T> getArtifactClass();
	public void create();
	public Node draw(T artifact);
}