# Maven/build changes

The canonical profiles live at repository level:

`docs/ai_agents/reporters/*.md`

Package them into the backend JAR by adding a resource:

```xml
<resource>
    <directory>${project.basedir}/../docs/ai_agents</directory>
    <targetPath>ai_agents</targetPath>
    <filtering>false</filtering>
</resource>
```

Add SnakeYAML:

```xml
<dependency>
    <groupId>org.yaml</groupId>
    <artifactId>snakeyaml</artifactId>
</dependency>
```

At runtime profiles become:

`classpath:/ai_agents/reporters/*.md`

When AI reporting is enabled and no profiles are found, startup should fail with a clear error.
