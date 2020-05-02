package be.nabu.eai.developer.plugin;

import be.nabu.eai.developer.MainController;
import be.nabu.eai.module.web.application.WebApplication;
import be.nabu.eai.repository.events.NodeEvent;
import be.nabu.eai.repository.events.NodeEvent.State;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class WebApplicationView {
	
	private ObservableList<WebApplication> applications;
	private BooleanProperty open = new SimpleBooleanProperty();

	public Node initialize(DataView view, MainController controller) {
		applications = FXCollections.observableArrayList(controller.getRepository().getArtifacts(WebApplication.class));
		controller.getRepository().getEventDispatcher().subscribe(NodeEvent.class, new be.nabu.libs.events.api.EventHandler<NodeEvent, Void>() {
			@Override
			public Void handle(NodeEvent event) {
				try {
					if (event.getState() == State.CREATE && event.isDone()) {
						if (event.getNode().getArtifact() instanceof WebApplication) {
							applications.add((WebApplication) event.getNode().getArtifact());
						}
					}
				}
				catch (Exception e) {
					controller.notify(e);
				}
				return null;
			}
		});
		
		VBox container = new VBox();
		
		VBox contentBox = new VBox();
		contentBox.managedProperty().bind(open);
		contentBox.visibleProperty().bind(open);
		
		Button create = new Button();
		create.setGraphic(MainController.loadFixedSizeGraphic("developer-data/add.png", 12));
		create.addEventHandler(ActionEvent.ANY, new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent arg0) {
				
			}
		});
		
		container.getChildren().addAll(
			view.newTitleBox("developer-data/website.png", "Applications", open, create), 
			contentBox
		);
		
		for (WebApplication application : applications) {
			HBox entryBox = view.newArtifactBox(application, null);
			contentBox.getChildren().add(entryBox);
		}
		return container;
	}
}
