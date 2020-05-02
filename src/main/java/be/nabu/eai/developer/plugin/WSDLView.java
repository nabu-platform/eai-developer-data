package be.nabu.eai.developer.plugin;

import be.nabu.eai.developer.plugin.api.ArtifactViewer;
import be.nabu.eai.module.wsdl.client.WSDLClient;
import javafx.scene.Node;

public class WSDLView implements ArtifactViewer<WSDLClient> {

	@Override
	public String getName() {
		return "Connectors (SOAP)";
	}

	@Override
	public String getGraphic() {
		return "developer-data/connectors.png";
	}

	@Override
	public Class<WSDLClient> getArtifactClass() {
		return WSDLClient.class;
	}

	@Override
	public void create() {
		
	}

	@Override
	public Node draw(String project, WSDLClient artifact) {
		return null;
	}

}
