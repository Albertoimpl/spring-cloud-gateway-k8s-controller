package com.example.springcloudgatewayk8scontroller;

import javax.annotation.PostConstruct;

import reactor.core.publisher.Mono;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.appbroker.deployer.AppDeployer;
import org.springframework.cloud.appbroker.deployer.DeployApplicationRequest;
import org.springframework.cloud.servicebroker.model.catalog.Plan;
import org.springframework.cloud.servicebroker.model.catalog.ServiceDefinition;
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceResponse;
import org.springframework.cloud.servicebroker.service.ServiceInstanceService;
import org.springframework.stereotype.Service;

@SpringBootApplication
public class SpringCloudGatewayK8sControllerApplication implements CommandLineRunner {

	@Autowired
	SpringCloudGatewayControlPlaneWithAppBroker scg;

	@Override
	public void run(String... args) throws Exception {
		scg.onCreate().block();
		scg.onBind().block();
	}

	public static void main(String[] args) {
		SpringApplication.run(SpringCloudGatewayK8sControllerApplication.class, args);
	}

}

