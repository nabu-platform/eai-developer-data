package be.nabu.eai.developer.plugin;

import be.nabu.eai.developer.plugin.api.ArtifactViewer;
import be.nabu.eai.repository.api.Entry;
import be.nabu.libs.services.vm.api.VMService;
import javafx.scene.Node;

public class ServiceView implements ArtifactViewer<VMService> {

	@Override
	public String getName() {
		return "Services";
	}

	@Override
	public String getSectionGraphic() {
		return "developer-data/service_big.png";
	}

	@Override
	public Class<VMService> getArtifactClass() {
		return VMService.class;
	}

	@Override
	public void create() {
		
	}

	@Override
	public Node draw(Entry project, VMService artifact) {
		return null;
	}

	@Override
	public boolean allow(Entry project, String artifactId) {
		return artifactId.startsWith(project.getId() + ".services.");
	}

}
