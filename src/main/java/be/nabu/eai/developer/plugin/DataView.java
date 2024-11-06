/*
* Copyright (C) 2020 Alexander Verbruggen
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU Lesser General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public License
* along with this program. If not, see <https://www.gnu.org/licenses/>.
*/

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
import be.nabu.eai.developer.components.RepositoryBrowser;
import be.nabu.eai.developer.plugin.api.ArtifactViewer;
import be.nabu.eai.developer.util.Confirm;
import be.nabu.eai.developer.util.Confirm.ConfirmType;
import be.nabu.eai.repository.EAINode;
import be.nabu.eai.repository.EAIRepositoryUtils;
import be.nabu.eai.repository.api.Entry;
import be.nabu.eai.repository.api.ResourceEntry;
import be.nabu.eai.repository.resources.RepositoryEntry;
import be.nabu.jfx.control.tree.drag.TreeDragDrop;
import be.nabu.libs.artifacts.api.Artifact;
import be.nabu.libs.services.api.DefinedService;
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
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class DataView implements DeveloperPlugin {

	public static final Insets MARGIN_LEFT = new Insets(0, 0, 0, 10);
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
					if (arg0.getClickCount() == 2) {
						open.set(!open.get());
					}
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
	
	public static void visible(boolean visible, Node...nodes) {
		visible(visible, Arrays.asList(nodes));
	}
	public static void visible(boolean visible, List<Node> nodes) {
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
		
		// custom buttons are first
		if (buttons != null) {
			buttonBox.getChildren().addAll(buttons);
		}
		
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
		Button view = new Button();
		view.setGraphic(MainController.loadFixedSizeGraphic("right-chevron.png", 12));
		view.addEventHandler(ActionEvent.ANY, new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent arg0) {
				MainController.getInstance().open(artifact.getId());					
			}
		});
		buttonBox.getChildren().add(view);
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
				if (artifactOpen != null && arg0.getClickCount() == 2) {
					artifactOpen.set(!artifactOpen.get());
				}
				if (selectedBox != null) {
					selectedBox.getStyleClass().remove("selected-box");
				}
				selectedBox = box;
				selectedBox.getStyleClass().add("selected-box");
			}
		});
		
		// we can always drag the boxes, we made it compatible with the repository tree for maximum reuse
		box.addEventHandler(MouseEvent.DRAG_DETECTED, new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent arg0) {
				Dragboard dragboard = box.startDragAndDrop(TransferMode.MOVE);
				ClipboardContent clipboard = new ClipboardContent();
				clipboard.put(TreeDragDrop.getDataFormat(RepositoryBrowser.getDataType(artifact.getClass())), artifact.getId());
				dragboard.setContent(clipboard);
			}
		});
		
		return box;
	}
	
	private void findProjects(Entry parent, List<Entry> projects) {
		for (Entry child : parent) {
			if (EAIRepositoryUtils.isProject(child) && "standard".equals(child.getCollection().getSubType())) {
				projects.add(child);
			}
			// can't have two projects (of this type) in one another
			else {
				findProjects(child, projects);
			}
		}
	}
	
	@Override
	public void initialize(MainController controller) {
		MainController.registerStyleSheet("developer-data.css");
		List<Entry> projects = new ArrayList<Entry>();
		findProjects(controller.getRepository().getRoot(), projects);
		for (Entry child : projects) {
			loadProject(controller, child);
		}
		// we always add a tab to create a new project
		Tab tab = new Tab("New Project");
		// just select the first tab by default
		controller.getTabBrowsers().getSelectionModel().select(controller.getTabBrowsers().getTabs().get(0));
		tab.setGraphic(MainController.loadFixedSizeGraphic("developer-data/add.png", 10));
		controller.getTabBrowsers().getTabs().add(controller.getTabBrowsers().getTabs().size() - 1, tab);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void loadProject(MainController controller, Entry projectEntry) {
		Tab tab = new Tab(NamingConvention.UPPER_TEXT.apply(projectEntry.getCollection().getName(), NamingConvention.LOWER_CAMEL_CASE));
		tab.setId(projectEntry.getId());
		controller.getTabBrowsers().getTabs().add(0, tab);
		
		ScrollPane scroll = new ScrollPane();
		scroll.setFitToWidth(true);
		VBox box = new VBox();
		scroll.setContent(box);
		tab.setContent(scroll);
		
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
			contentBox.getStyleClass().add("developer-content-box");
			contentBox.managedProperty().bind(viewOpen);
			contentBox.visibleProperty().bind(viewOpen);
			
			container.getChildren().addAll(
				newTitleBox(viewer.getSectionGraphic(), viewer.getName(), viewOpen, create), 
				contentBox
			);
			
			box.getChildren().add(container);
			
			List<?> artifacts = controller.getRepository().getArtifacts(viewer.getArtifactClass());
			for (Object artifact : artifacts) {
				String id = ((Artifact) artifact).getId();
				if (viewer.allow(projectEntry, id)) {
					Node draw = viewer.draw(projectEntry, (Artifact) artifact);
					VBox artifactContainer = new VBox();
					
					Button[] array = (Button[]) viewer.getButtons(projectEntry, (Artifact) artifact).toArray(new Button[0]);
					HBox artifactBox = newArtifactBox((Artifact) artifact, draw != null, array);
					
					if (viewer.getArtifactGraphic() != null) {
						Node graphic = MainController.loadFixedSizeGraphic(viewer.getArtifactGraphic(), 16, 25);
						artifactBox.getChildren().add(1, graphic);
					}
					
					artifactContainer.getChildren().add(artifactBox);
					if (draw != null) {
						artifactContainer.getChildren().add(draw);
						draw.managedProperty().bind(DataView.getInstance().getOpen(id));
						draw.visibleProperty().bind(DataView.getInstance().getOpen(id));
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
