package be.nabu.eai.developer.plugin;

import java.util.List;
import java.util.concurrent.ForkJoinPool;

import be.nabu.eai.api.NamingConvention;
import be.nabu.eai.developer.plugin.api.ArtifactViewer;
import be.nabu.eai.module.jdbc.pool.JDBCPoolArtifact;
import be.nabu.eai.module.jdbc.pool.JDBCPoolClientUtils;
import be.nabu.eai.module.services.crud.CRUDArtifact;
import be.nabu.eai.module.services.crud.CRUDService.CRUDType;
import be.nabu.libs.property.ValueUtils;
import be.nabu.libs.types.api.DefinedType;
import be.nabu.libs.types.properties.CollectionNameProperty;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import nabu.protocols.jdbc.pool.types.TableDescription;

/**
 * Database file layout by default:
 * 
 * <project>.database.<name>.connection -> connection
 * <project>.database.<name>.type.<tableName>
 * <project>.database.<name>.crud.<tableName>
 */
public class DatabaseView implements ArtifactViewer<JDBCPoolArtifact> {

	private List<TableDescription> listTables;
	
	@Override
	public Class<JDBCPoolArtifact> getArtifactClass() {
		return JDBCPoolArtifact.class;
	}

	@Override
	public Node draw(String project, JDBCPoolArtifact artifact) {
		VBox box = new VBox();
		ProgressIndicator indicator = new ProgressIndicator();
		box.getChildren().add(indicator);
		ForkJoinPool.commonPool().submit(new Runnable() {
			@Override
			public void run() {
				if (listTables == null) {
					listTables = JDBCPoolClientUtils.listTables(artifact);
				}
				Platform.runLater(new Runnable() {
					@Override
					public void run() {
						box.getChildren().clear();
						// this is by far the most compatible with "manually built" applications by not having to assume positions at this point
						List<DefinedType> types = artifact.getConfig().getManagedTypes();
						if (types != null) {
							for (DefinedType type : types) {
								// we are currently? only interested in types that live in our project
								// otherwise we might be faced with a ton of types from cms, analysis,...
								if (type == null || !type.getId().startsWith(project + ".")) {
									continue;
								}
								
								String collectionName = ValueUtils.getValue(CollectionNameProperty.getInstance(), type.getProperties());
								if (collectionName == null) {
									collectionName = type.getName();
								}
								if (collectionName != null) {
									collectionName = NamingConvention.UNDERSCORE.apply(collectionName, NamingConvention.UPPER_CAMEL_CASE);
								}
								VBox childBox = new VBox();
								childBox.managedProperty().bind(DataView.getInstance().getOpen(artifact.getId()));
								childBox.visibleProperty().bind(DataView.getInstance().getOpen(artifact.getId()));
								
								// synchronize button? which direction?
								HBox newArtifactBox = DataView.getInstance().newArtifactBox(type, true);
								newArtifactBox.getStyleClass().add("first-child-entry");
								childBox.getChildren().add(newArtifactBox);
								
								VBox content = new VBox();
								content.managedProperty().bind(DataView.getInstance().getOpen(type.getId()));
								content.visibleProperty().bind(DataView.getInstance().getOpen(type.getId()));
								
								// for each data type you can have 5 services: get/list/delete/update/read
								content.getChildren().addAll(
									buildDraggableOperation(type, CRUDType.GET),
									buildDraggableOperation(type, CRUDType.LIST),
									buildDraggableOperation(type, CRUDType.CREATE),
									buildDraggableOperation(type, CRUDType.UPDATE),
									buildDraggableOperation(type, CRUDType.DELETE)
								);
								childBox.getChildren().add(content);
								box.getChildren().add(childBox);
							}
							// at the very end we need an "add" button to add existing tables
						}
					}
				});
			}
		});
		return box;
	}
	
	private HBox buildDraggableOperation(DefinedType type, CRUDType operation) {
		HBox box = new HBox();
		box.getChildren().add(new Label(operation.name()));
		return box;
	}
	
	private CRUDArtifact getCrudFor(String project, JDBCPoolArtifact artifact, DefinedType type) {
		// we search for a CRUD provider for the given type
		for (CRUDArtifact crud : artifact.getRepository().getArtifacts(CRUDArtifact.class)) {
			if (type.equals(crud.getConfig().getCoreType()) && crud.getId().startsWith(project + ".")) {
				return crud;
			}
		}
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
