# What is Cartridge?

A Cartridge is a set of resources that are loaded into the Platform for a particular project. They may contain anything from a simple reference implementation for a technology to a set of best practice examples for building, deploying, and managing a technology stack that can be used by a project.

This cartridge consists of source code repositories and Jenkins jobs.

## Source code repositories

Cartridge loads the source code repositories:

* [Sample cookbook - Vim](https://github.com/accenture/adop-cartridge-chef-reference.git)
* [Scripts used by this cartridge - adop-cartridge-chef-scripts](https://github.com/accenture/adop-cartridge-chef-scripts.git)

## Jenkins Jobs

This cartridge provides the "[Seed job](#seed-job)", which generates the Jenkins jobs and pipeline view to -

* Detect cookbook changes
* Perform Sanity checks (Ruby syntax, JSON syntax, Berksfile dependencies)
* Run Unit tests (Chefspec)
* Perform Static code analysis (Foodcritc & Cookstyle)
* Run Integration tests via Kitchen converge (Docker driver by default)
* Upload cookbook on Chef server (**Chef server settings may be skipped**, optional step)

### Seed job

Whenever you load the cartridge successfully, as end result, you will get only one Jenkins job - "Generate_Chef_Pipeline_Jobs" (this is what we call a Seed job), which you have to run to set-up your deployment job and generate rest of Jenkins jobs and a pipeline view.

List of parameters which you have to specify to use "Upload cookbook on Chef server" feature:

* CHEF_SERVER_ORGANIZATION_URL - We will use this url as endpoint for cookbook upload. This will be in the form of ``` https://<chef-server-url>/organizations/<organisation-name> ```
* CHEF_SERVER_SSH_USERNAME - Jenkins ssh username with private key credentials record. This can be obtained by accessing the user page from a URL such as ``` https://<chef-server-url>/users/admin ```, then resetting the private key and taking note of it.
* CHEF_SERVER_SSH_VALIDATOR - Jenkins ssh validator username with private key credentials record. This can be accessed by accessing the organisation URL from ``` https://<chef-server-url>/organizations/<organisation-name> ``` and resetting the validation key by clicking on the cog next to your organisation

This cartidge is designed to be used alongside the [ADOP chef platform extension](https://github.com/Accenture/adop-platform-extension-chef). The cartridge can, however, be used without the chef server for the purposes of demonstration. If you do not specify the above parameters in your seed job, the pipeline will still be generated but it will fail at the Upload cookbook to chef server step.

# License
Please view [license information](LICENSE.md) for the software contained on this image.

## Documentation
Documentation will be captured within this README.md and this repository's Wiki.

## Issues
If you have any problems with or questions about this image, please contact us through a [GitHub issue](https://github.com/Accenture/adop-cartridge-chef/issues).

## Contribute
You are invited to contribute new features, fixes, or updates, large or small; we are always thrilled to receive pull requests, and do our best to process them as fast as we can.

Before you start to code, we recommend discussing your plans through a [GitHub issue](https://github.com/Accenture/adop-cartridge-chef/issues), especially for more ambitious contributions. This gives other contributors a chance to point you in the right direction, give you feedback on your design, and help you find out if someone else is working on the same thing.
