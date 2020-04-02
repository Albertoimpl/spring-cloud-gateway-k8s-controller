package com.example.springcloudgatewayk8scontroller;

import reactor.core.publisher.Mono;

import org.springframework.cloud.appbroker.deployer.AppDeployer;
import org.springframework.cloud.appbroker.deployer.DeployApplicationRequest;
import org.springframework.stereotype.Service;

@Service
public class SpringCloudGatewayControlPlaneWithKubernetesClient {

	private AppDeployer kubernetesAppDeployer;

	public SpringCloudGatewayControlPlaneWithKubernetesClient(AppDeployer kubernetesAppDeployer) {
		this.kubernetesAppDeployer = kubernetesAppDeployer;
	}

	public Mono<Void> onCreate() {
		DeployApplicationRequest request =
			DeployApplicationRequest
				.builder()
				.name("my-gateway")
				.path("albertoimpl/vanilla-oss-gateway")
				.build();
		return kubernetesAppDeployer
			.deploy(request)
			.then();
	}

}
