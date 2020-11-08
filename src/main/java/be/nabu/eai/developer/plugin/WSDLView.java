package be.nabu.eai.developer.plugin;

import be.nabu.eai.developer.plugin.api.ArtifactViewer;
import be.nabu.eai.module.wsdl.client.WSDLClient;
import be.nabu.eai.repository.api.Entry;
import javafx.scene.Node;

public class WSDLView implements ArtifactViewer<WSDLClient> {

	@Override
	public String getName() {
		return "Connectors (SOAP)";
	}

	@Override
	public String getSectionGraphic() {
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
	public Node draw(Entry project, WSDLClient artifact) {
		return null;
	}

}
