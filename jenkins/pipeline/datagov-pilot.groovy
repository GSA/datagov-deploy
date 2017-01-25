
def run(environment) {
    initialize(environment)
    provision(environment)
    test(environment)
}

def initialize(environment) {
}

def provision(environment) {
    // def terraform = load "./jenkins/provisioner/terraform.groovy"
    // terraform.run('pilot', environment, "infrastructure")   

    // def playbook = load "./jenkins/provisioner/playbook.groovy"
    // def system = "datagov"
    // playbook.run("jumpbox", system, environment, "bastion", 
    //     "always,jumpbox,apache", "shibboleth")
    // playbook.run("datagov-web", system, environment, "wordpress-web",
    // 	null, "trendmicro,vim,deploy,deploy-rollback,secops,postfix")
}

def test(environment, outputDirectory) {
    // TODO Should get IPs from stack or ansible ec2.py,
    //      rather than have to discovering them
    echo "Define environment file in ${pwd()}; write output to ${outputDirectory}"
    def environmentFile = "${pwd()}/${environment}-input.json"
    echo "Create environment file ${environmentFile} "
    def ips = discoverPublicIps(environment, 'wordpress-web')

    echo "Found ips=|${ips}|"

    dir ("./postman/pilot") {
        sh "ls -al"
        sh "cat ./environment-template.json"
        for (ip in ips) {
            def command = "cat ./environment-template.json | " + 
                "sed -e 's|__WORDPRESS_WEB_HOST__|${ip}|g' > " +
                "${environmentFile}"
            echo "About to run [${command}]"
            sh command
            sh "cat ${environmentFile}"
            echo "Run test"
            runTest("verify-pilot", environmentFile, outputDirectory)
        }
    }
}


def runTest(testName, environmentFile, outputDirectory) {
    def arguments = [
        "./${testName}.json",
        "-e ${environmentFile}",
        " --reporters junit,cli",
        " --reporter-junit-export ${outputDirectory}/TEST-${testName}.xml"
    ]
    def command = "newman run ${arguments.join(' ')}"
    echo "About to run [${command}]"
    sh command
}

def discoverPublicIps(environment, resource) {
    def ips = []
    def results = sh (
            returnStdout: true, 
            script: """
                aws ec2 describe-instances \
                    --region ${env.AWS_REGION} \
                    --filter "Name=tag:System,Values=datagov" \
                             \"Name=tag:Stack,Values=pilot\" \
                             \"Name=tag:Environment,Values=${environment}\" \
                             \"Name=tag:Resource,Values=${resource}\" \
                             \"Name=instance-state-name,Values=running\" \
                    --query \"Reservations[].Instances[].{Ip:PublicIpAddress}\" \
                    --output text
            """).split('\n')
    for (String r: results) {
        r = r.trim()
        if (r) {
            ips << r
        }
    }
    return ips
}

return this