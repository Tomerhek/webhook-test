node{
  stage('SCM Checkout'){
    git 'https://github.com/Tomerhek/webhook-test'
  }
  stage('Compile-Package'){
    sh 'mvn package'
  }
}
