package be.nabu.eai.developer.plugin;

import be.nabu.eai.developer.MainController;
import be.nabu.eai.module.jdbc.pool.JDBCPoolArtifact;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.layout.VBox;

public class DatabaseView {
	private ObservableList<JDBCPoolArtifact> pools;
	private BooleanProperty open = new SimpleBooleanProperty();
	
	public Node initialize(DataView view, MainController controller) {
		VBox container = new VBox();
		
		VBox contentBox = new VBox();
		contentBox.managedProperty().bind(open);
		contentBox.visibleProperty().bind(open);
		
		container.getChildren().addAll(
			view.newTitleBox("developer-data/database.png", "Databases", open), 
			contentBox
		);
		
		return container;
	}
}
