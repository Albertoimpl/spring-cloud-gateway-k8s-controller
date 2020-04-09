package com.example.springcloudgatewayk8scontroller;

import java.util.Collections;

import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.models.V1Container;
import io.kubernetes.client.openapi.models.V1Deployment;
import io.kubernetes.client.openapi.models.V1DeploymentList;
import io.kubernetes.client.openapi.models.V1DeploymentSpec;
import io.kubernetes.client.openapi.models.V1LabelSelector;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1PodSpec;
import io.kubernetes.client.openapi.models.V1PodTemplateSpec;
import io.kubernetes.client.openapi.models.V1Status;

class DeploymentService {

	private AppsV1Api appsV1Api;

	public DeploymentService(AppsV1Api appsV1Api) {
		this.appsV1Api = appsV1Api;
	}

	V1DeploymentList listDeployments() throws ApiException {
		System.out.println("Listing deployments");
		V1DeploymentList deploymentList =
			appsV1Api.listNamespacedDeployment(
				"default",
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null);
		System.out.println("Listed deployments");
		System.out.println(deploymentList.getItems().size());
		return deploymentList;
	}


	void createDeployment() throws ApiException {
		System.out.println("Creating deployment");
		String serviceInstanceId = "quack";

		V1Deployment deployment = new V1Deployment();
		deployment.setApiVersion("apps/v1");
		deployment.setKind("Deployment");

		V1ObjectMeta metadata = new V1ObjectMeta();
		metadata.setName("vanilla-oss-gateway-deployment");
		deployment.setMetadata(metadata);

		V1DeploymentSpec deploymentSpec = new V1DeploymentSpec();

		V1LabelSelector selector = new V1LabelSelector();
		selector.setMatchLabels(Collections.singletonMap("service-instance-id", serviceInstanceId));
		selector.setMatchLabels(Collections.singletonMap("app", "vanilla-oss-gateway"));
		deploymentSpec.setSelector(selector);

		V1PodTemplateSpec template = new V1PodTemplateSpec();

		V1PodSpec podSpec = new V1PodSpec();
		V1Container v1Container = new V1Container();
		v1Container.setName("vanilla-oss-gateway");
		v1Container.setImage("albertoimpl/vanilla-oss-gateway");
		podSpec.setContainers(Collections.singletonList(v1Container));
		template.setSpec(podSpec);

		V1ObjectMeta podMetadata = new V1ObjectMeta();
		podMetadata.setLabels(Collections.singletonMap("app", "vanilla-oss-gateway"));
		template.setMetadata(podMetadata);

		deploymentSpec.setTemplate(template);

		deployment.setSpec(deploymentSpec);

		appsV1Api.createNamespacedDeployment(
			"default",
			deployment,
			null,
			null,
			null
		);

		System.out.println("Created deployment");
	}

	void deleteDeployment() throws ApiException {
		System.out.println("Deleting deployment");
		V1Status status = appsV1Api.deleteNamespacedDeployment(
			"vanilla-oss-gateway-deployment",
			"default",
			null,
			null,
			null,
			null,
			null,
			null
		);
		System.out.println("Deleted deployment");
		System.out.println(status);
	}

}
