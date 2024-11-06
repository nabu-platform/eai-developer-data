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
