rootProject.name = "rewrite-java-project"
include("agrona")
include("agrona:agrona-samples")
findProject(":agrona:agrona-samples")?.name = "agrona-samples"
include("agrona:rewrite-agrona")
findProject(":agrona:rewrite-agrona")?.name = "rewrite-agrona"
