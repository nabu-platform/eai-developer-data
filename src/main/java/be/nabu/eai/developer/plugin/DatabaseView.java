package be.nabu.eai.developer.plugin;

import be.nabu.eai.developer.plugin.api.ArtifactViewer;
import be.nabu.eai.module.jdbc.pool.JDBCPoolArtifact;
import javafx.scene.Node;

public class DatabaseView implements ArtifactViewer<JDBCPoolArtifact> {
	
	@Override
	public Class<JDBCPoolArtifact> getArtifactClass() {
		return JDBCPoolArtifact.class;
	}

	@Override
	public Node draw(JDBCPoolArtifact artifact) {
		return null;
	}

	@Override
	public String getGraphic() {
		return "developer-data/database.png";
	}

	@Override
	public String getName() {
		return "Databases";
	}

	@Override
	public void create() {
		
	}
}
