
Setup:
```shell script
k delete springcloudgateways my-gateway-with-crd
k delete deployment.apps/vanilla-oss-gateway-deployment
k delete networkpolicy/network-policy-quack
k delete customresourcedefinitions springcloudgateways.tanzu.vmware.com

k apply -f spring-cloud-gateway-custom-resource-definition.yaml

k get all
k get networkpolicies
k get scgs
```

Start `SpringCloudGatewayControlPlaneWithCRDReconciler`
```shell script
k apply -f my-gateway-with-crd.yaml

k get all
k get networkpolicies
k get scgs


k delete scg my-gateway-with-crd

k get all
k get networkpolicies
k get scgs
```
