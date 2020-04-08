package com.vmware.tanzu.springcloudgateway;

import java.util.ArrayList;
import java.util.List;

import io.kubernetes.client.openapi.models.V1ListMeta;

public class V1SpringCloudGatewayList {


	private V1ListMeta metadata;

	private List<V1SpringCloudGateway> items = new ArrayList<>();

	public V1SpringCloudGatewayList(V1ListMeta metadata,
		List<V1SpringCloudGateway> items) {
		this.metadata = metadata;
		this.items = items;
	}

	public V1ListMeta getMetadata() {
		return metadata;
	}

	public void setMetadata(V1ListMeta metadata) {
		this.metadata = metadata;
	}

	public List<V1SpringCloudGateway> getItems() {
		return items;
	}

	public void setItems(List<V1SpringCloudGateway> items) {
		this.items = items;
	}

}
