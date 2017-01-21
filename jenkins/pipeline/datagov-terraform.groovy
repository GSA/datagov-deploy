
def run(environment) {
    initialize(environment)
    provision(environment)
    test(environment)
}

def initialize(environment) {
}

def provision(environment) {
    def terraform = load "./jenkins/provisioner/terraform.groovy"
    terraform.run('infrastructure', environment)   
    terraform.run('pilot', environment, "infrastructure")   
}

def test(environment) {
    // Nothing here yet
}


return this