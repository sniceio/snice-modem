package io.snice.old_modem.framers;

import io.snice.buffer.Buffer;
import io.snice.old_modem.modem.Config;
import io.snice.old_modem.response.Response;
import io.snice.old_modem.response.Result;

import java.util.List;
import java.util.Optional;

import static io.snice.preconditions.PreConditions.assertNotNull;

/**
 * Depending on the settings of the modem, the responses will be formatted differently. Therefore
 * you will need to create a new framer whenever you change any of the settings that affect
 * the formatting.
 */
public interface Framer {

    static Framer from(final Config config) {
        assertNotNull(config, "The configuration cannot be null");
        final InfoResponseFramer infoFramer = InfoResponseFramer.create(config.getS3(), config.getS4(), config.isVerboseResponseFormat());
        final ResultResponseFramer resultFramer = ResultResponseFramer.create(config.getS3(), config.getS4(), config.isVerboseResponseFormat());
        return new DefaultFramer(config, infoFramer, resultFramer);
    }

    Optional<Response> frame(final Buffer buffer);

    class DefaultFramer implements Framer {

        private final Config config;
        private final InfoResponseFramer infoFramer;
        private final ResultResponseFramer resultFramer;

        private DefaultFramer(final Config config, final InfoResponseFramer infoFramer, final ResultResponseFramer resultFramer) {
            this.config = config;
            this.infoFramer = infoFramer;
            this.resultFramer = resultFramer;
        }

        @Override
        public Optional<Response> frame(final Buffer buffer) {
            final Optional<Result> resultCode = resultFramer.match(buffer);
            resultCode.ifPresent(code -> {
                System.err.println("Yep, an Result response: " + code.getCode());
            });

            if (infoFramer.match(buffer)) {
                System.err.println("Yep, an INFO response");
            }

            return Optional.empty();
        }
    }


    interface ResultResponseFramer {

        Optional<Result> match(final Buffer buffer);

        static ResultResponseFramer create(final char S3, final char S4, final boolean verbose) {
            if (verbose) {
                return new VerboseResultResponseFramer(S3, S4);
            }

            return new QuietResultResponseFramer(S3, S4);
        }

        class VerboseResultResponseFramer implements ResultResponseFramer {
            private final List<Result> codes;

            private VerboseResultResponseFramer(final char S3, final char S4) {
                codes = Result.initialize(true, S3, S4);
            }

            @Override
            public Optional<Result> match(final Buffer buffer) {
                return codes.stream().filter(code -> code.match(buffer)).findFirst();
            }
        }

        class QuietResultResponseFramer implements ResultResponseFramer {
            private QuietResultResponseFramer(final char S3, final char S4) {
            }

            /*
            @Override
            public boolean match(final byte[] buffer, final int offset, final int length) {
                try {
                    final int last = offset + length - 1;
                    final boolean trailing = buffer[last] == S4 && buffer[last - 1] == S3;
                    if (!trailing) {
                        return false;
                    }

                    final int code = buffer[last - 2];
                    System.err.println("V0 result code: " + code);
                    return true;
                } catch (final ArrayIndexOutOfBoundsException e) {
                    return false;
                }
            }
            */

            @Override
            public Optional<Result> match(final Buffer buffer) {
                System.err.println("why is this one used");
                return Optional.empty();
            }
        }

    }

    interface InfoResponseFramer {

        boolean match(final byte[] buffer, final int offset, final int length);

        boolean match(final Buffer buffer);

        static InfoResponseFramer create(final char S3, final char S4, final boolean verbose) {
            if (verbose) {
                return new VerboseInfoResponseFramer(S3, S4);
            }

            return new QuietInfoResponseFramer(S3, S4);
        }

        class VerboseInfoResponseFramer implements InfoResponseFramer {
            private final char S3;
            private final char S4;

            private VerboseInfoResponseFramer(final char S3, final char S4) {
                this.S3 = S3;
                this.S4 = S4;
            }

            @Override
            public boolean match(final byte[] buffer, final int offset, final int length) {
                try {
                    final int last = offset - 1 + length;
                    final boolean leading = buffer[offset] == S3 && buffer[offset +1] == S4;
                    final boolean trailing = buffer[last - 1] == S3 && buffer[last] == S4;

                    // System.err.println("Verbose checking " + offset + " length " + length);
                    // System.err.println("Leading:  " + leading);
                    // System.err.println("Trailing:  " + trailing);

                    return leading && trailing;
                } catch (final ArrayIndexOutOfBoundsException e) {
                    e.printStackTrace();
                    return false;
                }
            }

            @Override
            public boolean match(final Buffer buffer) {
                return false;
            }
        }

        class QuietInfoResponseFramer implements InfoResponseFramer {
            private final char S3;
            private final char S4;

            private QuietInfoResponseFramer(final char S3, final char S4) {
                this.S3 = S3;
                this.S4 = S4;
            }

            @Override
            public boolean match(final byte[] buffer, final int offset, final int length) {
                try {
                    final int last = offset + length;
                    return buffer[last - 1] == S3 && buffer[last] == S4;
                } catch (final ArrayIndexOutOfBoundsException e) {
                    return false;
                }
            }

            @Override
            public boolean match(final Buffer buffer) {
                return false;
            }
        }



    }
}
