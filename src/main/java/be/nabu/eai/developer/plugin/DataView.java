package be.nabu.eai.developer.plugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

import be.nabu.eai.api.NamingConvention;
import be.nabu.eai.developer.MainController;
import be.nabu.eai.developer.api.DeveloperPlugin;
import be.nabu.eai.developer.plugin.api.ArtifactViewer;
import be.nabu.eai.developer.util.Confirm;
import be.nabu.eai.developer.util.Confirm.ConfirmType;
import be.nabu.eai.repository.EAINode;
import be.nabu.eai.repository.api.Entry;
import be.nabu.eai.repository.api.ResourceEntry;
import be.nabu.eai.repository.resources.RepositoryEntry;
import be.nabu.libs.artifacts.api.Artifact;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class DataView implements DeveloperPlugin {

	private static final Insets MARGIN_LEFT = new Insets(0, 0, 0, 10);
	private Artifact selected;
	private HBox selectedBox;
	private static DataView instance;
	
	public DataView() {
		instance = this;
	}
	
	private Map<String, BooleanProperty> open = new HashMap<String, BooleanProperty>();

	public BooleanProperty getOpen(String id) {
		if (!open.containsKey(id)) {
			open.put(id, new SimpleBooleanProperty());
		}
		return open.get(id);
	}
	
	public HBox newTitleBox(String graphic, String title, BooleanProperty open, Button...buttons) {
		HBox box = new HBox();
		box.getStyleClass().addAll("developer-title-box");
		Node graph = MainController.loadFixedSizeGraphic(graphic, 32);
		graph.getStyleClass().add("title-graphic");
		HBox.setMargin(graph, MARGIN_LEFT);
		box.getChildren().add(graph);
		Label label = new Label(title);
		label.getStyleClass().add("title-text");
		label.setMaxWidth(Double.MAX_VALUE);
		HBox.setHgrow(label, Priority.ALWAYS);
		HBox.setMargin(label, MARGIN_LEFT);
		box.getChildren().add(label);
		if (buttons != null) {
			HBox buttonBox = new HBox();
			HBox.setMargin(buttonBox, MARGIN_LEFT);
			HBox.setHgrow(buttonBox, Priority.NEVER);
			buttonBox.getChildren().addAll(buttons);
			box.getChildren().add(buttonBox);
		}
		if (open != null) {
			ImageView opened = MainController.loadGraphic(open.get() ? "minus.png" : "plus.png");
			box.getChildren().add(0, opened);
			box.addEventHandler(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {
				@Override
				public void handle(MouseEvent arg0) {
					open.set(!open.get());
				}
			});
			open.addListener(new ChangeListener<Boolean>() {
				@Override
				public void changed(ObservableValue<? extends Boolean> arg0, Boolean arg1, Boolean arg2) {
					ImageView opened = MainController.loadGraphic(arg2 ? "minus.png" : "plus.png");
					box.getChildren().set(0, opened);
				}
			});
		}
		return box;
	}
	
	private void visible(boolean visible, Node...nodes) {
		visible(visible, Arrays.asList(nodes));
	}
	private void visible(boolean visible, List<Node> nodes) {
		for (Node node : nodes) {
			node.setManaged(visible);
			node.setVisible(visible);
		}
	}
	public HBox newArtifactBox(Artifact artifact, boolean canOpen, Button...buttons) {
		SimpleBooleanProperty artifactOpen = canOpen ? new SimpleBooleanProperty() : null;
		if (artifactOpen != null) {
			open.put(((Artifact) artifact).getId(), artifactOpen);
		}
		
		Entry entry = MainController.getInstance().getRepository().getEntry(artifact.getId());
		
		String title = entry.getNode().getName() == null ? artifact.getId() : entry.getNode().getName();
		String description = entry.getNode().getDescription();
		
		HBox box = new HBox();
		box.getStyleClass().add("developer-entry-box");
		
		VBox content = new VBox();
		Label titleLabel = new Label(title);
		titleLabel.getStyleClass().add("entry-title");
		content.getChildren().add(titleLabel);
		content.setAlignment(Pos.CENTER_LEFT);
		
		Label descriptionLabel = new Label();
		descriptionLabel.getStyleClass().add("entry-description");
		descriptionLabel.setWrapText(true);
		if (description != null) {
			descriptionLabel.setText(description);
		}
		
		Label idLabel = new Label(artifact.getId());
		idLabel.setText(artifact.getId());
		content.getChildren().add(idLabel);
		idLabel.getStyleClass().add("entry-id");
		visible(!idLabel.getText().equals(titleLabel.getText()), idLabel);
		visible(!descriptionLabel.getText().trim().isEmpty(), descriptionLabel);
		
		content.getChildren().add(descriptionLabel);
		
		box.getChildren().add(content);
		HBox.setMargin(content, MARGIN_LEFT);
		HBox.setHgrow(content, Priority.ALWAYS);
		content.setMaxWidth(Double.MAX_VALUE);
		
		HBox buttonBox = new HBox();
		HBox.setMargin(buttonBox, MARGIN_LEFT);
		HBox.setHgrow(buttonBox, Priority.NEVER);
		
		if (entry instanceof RepositoryEntry) {
			Button edit = new Button();
			edit.setGraphic(MainController.loadFixedSizeGraphic("developer-data/edit.png", 12));
			edit.addEventHandler(ActionEvent.ANY, new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent arg0) {
					visible(false, content.getChildren());
					visible(false, buttonBox.getChildren());
					
					TextField titleField = new TextField(titleLabel.getText());
					TextArea descriptionField = new TextArea(descriptionLabel.getText());
					content.getChildren().addAll(titleField, descriptionField);
					
					Button save = new Button();
					Button cancel = new Button();

					EventHandler<ActionEvent> cancelEvent = new EventHandler<ActionEvent>() {
						@Override
						public void handle(ActionEvent arg0) {
							content.getChildren().removeAll(titleField, descriptionField);
							buttonBox.getChildren().removeAll(save, cancel);
							visible(true, content.getChildren());
							visible(true, buttonBox.getChildren());
							
							visible(!idLabel.getText().equals(titleLabel.getText()), idLabel);
							visible(!descriptionLabel.getText().trim().isEmpty(), descriptionLabel);
						}
					};
					
					save.setGraphic(MainController.loadFixedSizeGraphic("developer-data/save.png", 12));
					save.addEventHandler(ActionEvent.ANY, new EventHandler<ActionEvent>() {
						@Override
						public void handle(ActionEvent event) {
							EAINode node = ((RepositoryEntry) entry).getNode();
							node.setName(titleField.getText().trim().isEmpty() ? null : titleField.getText().trim());
							node.setDescription(descriptionField.getText().trim().isEmpty() ? null : descriptionField.getText().trim());
							// save changes
							((RepositoryEntry) entry).saveNode();
							// TODO: tell others about the change! maybe a new collaboration event to only update node?
							titleLabel.setText(node.getName() == null ? artifact.getId() : node.getName());
							descriptionLabel.setText(node.getDescription() == null ? "" : node.getDescription());
							cancelEvent.handle(event);
						}
					});
					cancel.addEventHandler(ActionEvent.ANY, cancelEvent);
					
					cancel.setGraphic(MainController.loadFixedSizeGraphic("developer-data/cancel.png", 12));
					
					buttonBox.getChildren().addAll(save, cancel);
				}
			});
			buttonBox.getChildren().add(edit);
			
			Button delete = new Button();
			delete.setGraphic(MainController.loadFixedSizeGraphic("developer-data/delete.png", 12));
			delete.addEventHandler(ActionEvent.ANY, new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent arg0) {
					MainController.getInstance().getRepositoryBrowser().remove((ResourceEntry) entry);
				}
			});
			buttonBox.getChildren().add(delete);
		}
		if (buttons != null) {
			buttonBox.getChildren().addAll(buttons);
		}
		box.getChildren().add(buttonBox);
		
		if (artifactOpen != null) {
			ImageView opened = MainController.loadGraphic(artifactOpen.get() ? "minus.png" : "plus.png");
			box.getChildren().add(0, opened);
			artifactOpen.addListener(new ChangeListener<Boolean>() {
				@Override
				public void changed(ObservableValue<? extends Boolean> arg0, Boolean arg1, Boolean arg2) {
					ImageView opened = MainController.loadGraphic(arg2 ? "minus.png" : "plus.png");
					box.getChildren().set(0, opened);
				}
			});
		}
		else {
			ImageView loadGraphic = MainController.loadGraphic("item.png");
			box.getChildren().add(0, loadGraphic);
		}
		box.addEventHandler(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent arg0) {
				if (artifactOpen != null) {
					artifactOpen.set(!artifactOpen.get());
				}
				if (selectedBox != null) {
					selectedBox.getStyleClass().remove("selected-box");
				}
				selectedBox = box;
				selectedBox.getStyleClass().add("selected-box");
				if (arg0.getClickCount() == 2) {
					MainController.getInstance().open(artifact.getId());
				}
			}
		});
		
		return box;
	}
	
	@Override
	public void initialize(MainController controller) {
		MainController.registerStyleSheet("developer-data.css");
		boolean first = true;
		for (Entry child : controller.getRepository().getRoot()) {
			if (!child.getName().equals("nabu")) {
				loadProject(controller, child.getName(), first);
				if (first) {
					first = false;
				}
			}
		}
		// we always add a tab to create a new project
		Tab tab = new Tab("New Project");
		tab.setGraphic(MainController.loadFixedSizeGraphic("developer-data/add.png", 10));
		controller.getTabBrowsers().getTabs().add(controller.getTabBrowsers().getTabs().size() - 1, tab);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void loadProject(MainController controller, String project, boolean select) {
		Tab tab = new Tab(NamingConvention.UPPER_TEXT.apply(project, NamingConvention.LOWER_CAMEL_CASE));
		tab.setId(project);
		controller.getTabBrowsers().getTabs().add(0, tab);
		if (select) {
			controller.getTabBrowsers().getSelectionModel().select(tab);
		}
		
		VBox box = new VBox();
		tab.setContent(box);
		
		List<ArtifactViewer> viewers = new ArrayList<ArtifactViewer>();
		for (ArtifactViewer viewer : ServiceLoader.load(ArtifactViewer.class)) {
			viewers.add(viewer);
		}
		viewers.sort(new Comparator<ArtifactViewer>() {
			@Override
			public int compare(ArtifactViewer o1, ArtifactViewer o2) {
				return o1.getName().compareTo(o2.getName());
			}
		});
		
		for (ArtifactViewer viewer : viewers) {
			SimpleBooleanProperty viewOpen = new SimpleBooleanProperty();
			open.put(viewer.getClass().getName(), viewOpen);
			Button create = new Button();
			create.setGraphic(MainController.loadFixedSizeGraphic("developer-data/add.png", 12));
			create.addEventHandler(ActionEvent.ANY, new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent arg0) {
					viewer.create();
				}
			});
			VBox container = new VBox();
			
			VBox contentBox = new VBox();
			contentBox.managedProperty().bind(viewOpen);
			contentBox.visibleProperty().bind(viewOpen);
			
			container.getChildren().addAll(
				newTitleBox(viewer.getGraphic(), viewer.getName(), viewOpen, create), 
				contentBox
			);
			
			box.getChildren().add(container);
			
			List<?> artifacts = controller.getRepository().getArtifacts(viewer.getArtifactClass());
			for (Object artifact : artifacts) {
				String id = ((Artifact) artifact).getId();
				if (viewer.allow(project, id)) {
					Node draw = viewer.draw(project, (Artifact) artifact);
					VBox artifactContainer = new VBox();
					HBox artifactBox = newArtifactBox((Artifact) artifact, draw != null);
					artifactContainer.getChildren().add(artifactBox);
					if (draw != null) {
						artifactContainer.getChildren().add(draw);
					}
					contentBox.getChildren().add(artifactContainer);
				}
			}
		}
	}

	public static DataView getInstance() {
		return instance;
	}
}
