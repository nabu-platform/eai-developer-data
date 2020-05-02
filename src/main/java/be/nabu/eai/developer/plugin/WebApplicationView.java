package be.nabu.eai.developer.plugin;

import be.nabu.eai.developer.plugin.api.ArtifactViewer;
import be.nabu.eai.module.web.application.WebApplication;
import javafx.scene.Node;

public class WebApplicationView implements ArtifactViewer<WebApplication> {
	
	@Override
	public String getName() {
		return "Applications";
	}

	@Override
	public String getSectionGraphic() {
		return "developer-data/website.png";
	}

	@Override
	public Class<WebApplication> getArtifactClass() {
		return WebApplication.class;
	}

	@Override
	public void create() {
		
	}

	@Override
	public Node draw(String project, WebApplication artifact) {
		return null;
	}
}
