package be.nabu.eai.developer.plugin;

import be.nabu.eai.developer.plugin.api.ArtifactViewer;
import be.nabu.eai.module.swagger.client.SwaggerClient;
import javafx.scene.Node;

public class SwaggerClientView implements ArtifactViewer<SwaggerClient> {

	@Override
	public String getName() {
		return "Connectors (REST)";
	}

	@Override
	public String getGraphic() {
		return "developer-data/connectors.png";
	}

	@Override
	public Class<SwaggerClient> getArtifactClass() {
		return SwaggerClient.class;
	}

	@Override
	public void create() {
		
	}

	@Override
	public Node draw(String project, SwaggerClient artifact) {
		return null;
	}

}
