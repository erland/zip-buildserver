package info.isaksson.erland.zipbuildserver.application.verification;

import info.isaksson.erland.zipbuildserver.domain.model.project.DetectedProject;
import info.isaksson.erland.zipbuildserver.domain.model.project.ProjectTechnology;
import info.isaksson.erland.zipbuildserver.domain.model.verification.NetworkMode;
import info.isaksson.erland.zipbuildserver.domain.model.verification.VerificationCommand;
import info.isaksson.erland.zipbuildserver.domain.model.verification.VerificationPlan;
import info.isaksson.erland.zipbuildserver.domain.model.verification.VerificationPlanSelection;
import jakarta.enterprise.context.ApplicationScoped;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class VerificationPlanService {
    private static final List<String> DEFAULT_PLAN_RESOURCES = List.of(
            "verification-plans/node-default.yml",
            "verification-plans/maven-default.yml",
            "verification-plans/multi-project-default.yml");

    private final List<VerificationPlan> plans;

    public VerificationPlanService() {
        this(DEFAULT_PLAN_RESOURCES.stream()
                .map(VerificationPlanService::loadResource)
                .map(VerificationPlanService::parsePlan)
                .toList());
    }

    public VerificationPlanService(List<VerificationPlan> plans) {
        this.plans = plans.stream()
                .filter(VerificationPlan::enabled)
                .sorted(Comparator.comparing(VerificationPlan::id))
                .toList();
    }

    public List<VerificationPlan> listPlans() {
        return plans;
    }

    public Optional<VerificationPlan> findById(String planId) {
        return plans.stream()
                .filter(plan -> plan.id().equals(planId))
                .findFirst();
    }

    public VerificationPlanSelection selectPlan(DetectedProject project) {
        return plans.stream()
                .filter(plan -> plan.technology() == project.technology())
                .findFirst()
                .map(plan -> VerificationPlanSelection.selected(
                        plan.id(),
                        plan.selectionReason() == null || plan.selectionReason().isBlank()
                                ? "Selected configured server-side verification plan."
                                : plan.selectionReason()))
                .orElseGet(() -> VerificationPlanSelection.skipped(
                        "No enabled server-side verification plan matched " + project.technology() + "."));
    }

    private static String loadResource(String resourceName) {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        try (InputStream input = classLoader.getResourceAsStream(resourceName)) {
            if (input == null) {
                throw new VerificationPlanParseException("Missing verification plan resource: " + resourceName);
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new VerificationPlanParseException("Unable to load verification plan resource: " + resourceName, exception);
        }
    }

    public static VerificationPlan parsePlan(String source) {
        PlanBuilder builder = new PlanBuilder();
        VerificationCommandBuilder commandBuilder = null;
        Section section = Section.ROOT;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new java.io.ByteArrayInputStream(source.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8))) {
            String rawLine;
            while ((rawLine = reader.readLine()) != null) {
                String line = stripComment(rawLine);
                if (line.isBlank()) {
                    continue;
                }
                String trimmed = line.trim();

                if (!line.startsWith(" ")) {
                    if (commandBuilder != null) {
                        builder.commands.add(commandBuilder.build());
                        commandBuilder = null;
                    }
                    if (trimmed.equals("indicators:")) {
                        section = Section.INDICATORS;
                    } else if (trimmed.equals("commands:")) {
                        section = Section.COMMANDS;
                    } else {
                        section = Section.ROOT;
                        applyKeyValue(builder, trimmed);
                    }
                    continue;
                }

                if (section == Section.INDICATORS && trimmed.startsWith("- ")) {
                    builder.indicators.add(unquote(trimmed.substring(2).trim()));
                    continue;
                }

                if (section == Section.COMMANDS) {
                    if (trimmed.startsWith("- label:")) {
                        if (commandBuilder != null) {
                            builder.commands.add(commandBuilder.build());
                        }
                        commandBuilder = new VerificationCommandBuilder();
                        commandBuilder.label = unquote(afterColon(trimmed));
                    } else if (commandBuilder != null) {
                        applyCommandKeyValue(commandBuilder, trimmed);
                    }
                }
            }
        } catch (IOException exception) {
            throw new VerificationPlanParseException("Unable to parse verification plan.", exception);
        }

        if (commandBuilder != null) {
            builder.commands.add(commandBuilder.build());
        }
        return builder.build();
    }

    private static void applyKeyValue(PlanBuilder builder, String line) {
        String value = unquote(afterColon(line));
        if (line.startsWith("id:")) {
            builder.id = value;
        } else if (line.startsWith("name:")) {
            builder.name = value;
        } else if (line.startsWith("technology:")) {
            builder.technology = ProjectTechnology.valueOf(value);
        } else if (line.startsWith("networkMode:")) {
            builder.networkMode = NetworkMode.valueOf(value);
        } else if (line.startsWith("enabled:")) {
            builder.enabled = Boolean.parseBoolean(value);
        } else if (line.startsWith("selectionReason:")) {
            builder.selectionReason = value;
        }
    }

    private static void applyCommandKeyValue(VerificationCommandBuilder builder, String line) {
        String value = unquote(afterColon(line));
        if (line.startsWith("workingDirectory:")) {
            builder.workingDirectory = value;
        } else if (line.startsWith("commandDisplay:")) {
            builder.commandDisplay = value;
        } else if (line.startsWith("timeoutSeconds:")) {
            builder.timeoutSeconds = Integer.parseInt(value);
        } else if (line.startsWith("optional:")) {
            builder.optional = Boolean.parseBoolean(value);
        }
    }

    private static String afterColon(String line) {
        int colon = line.indexOf(':');
        if (colon < 0) {
            return "";
        }
        return line.substring(colon + 1).trim();
    }

    private static String stripComment(String line) {
        int index = line.indexOf('#');
        return index >= 0 ? line.substring(0, index) : line;
    }

    private static String unquote(String value) {
        if ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'"))) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    private enum Section {
        ROOT,
        INDICATORS,
        COMMANDS
    }

    private static final class PlanBuilder {
        private String id;
        private String name;
        private ProjectTechnology technology;
        private final List<String> indicators = new ArrayList<>();
        private final List<VerificationCommand> commands = new ArrayList<>();
        private NetworkMode networkMode = NetworkMode.DEPENDENCY;
        private boolean enabled = true;
        private String selectionReason;

        private VerificationPlan build() {
            List<String> missing = new ArrayList<>();
            if (id == null) {
                missing.add("id");
            }
            if (name == null) {
                missing.add("name");
            }
            if (technology == null) {
                missing.add("technology");
            }
            if (!missing.isEmpty()) {
                throw new VerificationPlanParseException("Verification plan is missing required fields: " + missing);
            }
            if (commands.isEmpty()) {
                throw new VerificationPlanParseException("Verification plan '" + id + "' must define at least one command.");
            }
            return new VerificationPlan(id, name, technology, List.copyOf(indicators),
                    List.copyOf(commands), networkMode, enabled, selectionReason);
        }
    }

    private static final class VerificationCommandBuilder {
        private String label;
        private String workingDirectory = "${project.path}";
        private String commandDisplay;
        private int timeoutSeconds = 600;
        private boolean optional;

        private VerificationCommand build() {
            if (label == null || label.isBlank()) {
                throw new VerificationPlanParseException("Verification command is missing a label.");
            }
            if (commandDisplay == null || commandDisplay.isBlank()) {
                throw new VerificationPlanParseException("Verification command '" + label + "' is missing commandDisplay.");
            }
            return new VerificationCommand(label, workingDirectory, commandDisplay, timeoutSeconds, optional);
        }
    }
}
